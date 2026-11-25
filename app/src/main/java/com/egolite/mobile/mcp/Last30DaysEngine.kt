package com.egolite.mobile.mcp

import android.net.Uri
import com.egolite.mobile.browser.SpaceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

object Last30DaysEngine {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 15_000
        }
    }

    suspend fun research(
        topic: String,
        mode: String = "quick",
        requestedSources: List<String> = emptyList()
    ): JsonObject = coroutineScope {
        val isDeep = mode.equals("deep", ignoreCase = true)
        val perSourceLimit = if (isDeep) 8 else 5
        val maxSnippetChars = if (isDeep) 500 else 280
        val activeSources = if (requestedSources.isEmpty()) {
            listOf("reddit", "hn", "polymarket", "web")
        } else {
            requestedSources.map { it.lowercase().trim() }
        }

        val redditJob = if ("reddit" in activeSources) async { fetchReddit(topic, perSourceLimit, maxSnippetChars) } else null
        val hnJob = if ("hn" in activeSources) async { fetchHackerNews(topic, perSourceLimit, maxSnippetChars) } else null
        val polyJob = if ("polymarket" in activeSources) async { fetchPolymarket(topic, perSourceLimit) } else null
        val webJob = if ("web" in activeSources) async { fetchWebLite(topic, perSourceLimit, maxSnippetChars) } else null
        val xJob = if ("x" in activeSources) async { fetchXViaWebView(topic, perSourceLimit, maxSnippetChars) } else null

        buildJsonObject {
            put("topic", topic)
            put("mode", if (isDeep) "deep" else "quick")
            put("window_days", 30)
            put("sources", buildJsonObject {
                redditJob?.let { put("reddit", it.await()) }
                hnJob?.let { put("hacker_news", it.await()) }
                polyJob?.let { put("polymarket", it.await()) }
                webJob?.let { put("web", it.await()) }
                xJob?.let { put("x_webview", it.await()) }
            })
        }
    }

    private suspend fun fetchReddit(topic: String, limit: Int, maxChars: Int): JsonArray {
        return runCatching {
            val encoded = Uri.encode(topic)
            val body = httpClient.get("https://www.reddit.com/search.json?q=$encoded&t=month&sort=top&limit=$limit") {
                header("User-Agent", "android:com.egolite.mobile:v0.1 (research)")
            }.bodyAsText()
            val children = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject?.get("children")?.jsonArray
                ?: return buildJsonArray {}
            buildJsonArray {
                children.take(limit).forEach { el ->
                    val d = el.jsonObject["data"]?.jsonObject ?: return@forEach
                    add(buildJsonObject {
                        put("title", d["title"]?.jsonPrimitive?.contentOrNull.orEmpty().take(160))
                        put("subreddit", d["subreddit_name_prefixed"]?.jsonPrimitive?.contentOrNull.orEmpty())
                        put("upvotes", d["ups"]?.jsonPrimitive?.intOrNull ?: 0)
                        put("comments", d["num_comments"]?.jsonPrimitive?.intOrNull ?: 0)
                        put("snippet", d["selftext"]?.jsonPrimitive?.contentOrNull.orEmpty().replace(Regex("""\s+"""), " ").take(maxChars))
                        put("url", "https://www.reddit.com" + d["permalink"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    })
                }
            }
        }.getOrElse {
            buildJsonArray { add(buildJsonObject { put("error", "Reddit fetch failed: ${it.message}") }) }
        }
    }

    private suspend fun fetchHackerNews(topic: String, limit: Int, maxChars: Int): JsonArray {
        return runCatching {
            val cutoffSec = (System.currentTimeMillis() / 1000L) - TimeUnit.DAYS.toSeconds(30)
            val encoded = Uri.encode(topic)
            val url = "https://hn.algolia.com/api/v1/search?query=$encoded&numericFilters=created_at_i>$cutoffSec&hitsPerPage=$limit"
            val body = httpClient.get(url).bodyAsText()
            val hits = json.parseToJsonElement(body).jsonObject["hits"]?.jsonArray ?: return buildJsonArray {}
            buildJsonArray {
                hits.take(limit).forEach { el ->
                    val h = el.jsonObject
                    val objectId = h["objectID"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    add(buildJsonObject {
                        put("title", (h["title"]?.jsonPrimitive?.contentOrNull ?: h["story_title"]?.jsonPrimitive?.contentOrNull).orEmpty().take(160))
                        put("points", h["points"]?.jsonPrimitive?.intOrNull ?: 0)
                        put("comments", h["num_comments"]?.jsonPrimitive?.intOrNull ?: 0)
                        put("snippet", h["comment_text"]?.jsonPrimitive?.contentOrNull.orEmpty().replace("<[^>]*>".toRegex(), " ").take(maxChars))
                        put("url", h["url"]?.jsonPrimitive?.contentOrNull ?: "https://news.ycombinator.com/item?id=$objectId")
                    })
                }
            }
        }.getOrElse {
            buildJsonArray { add(buildJsonObject { put("error", "HN fetch failed: ${it.message}") }) }
        }
    }

    private suspend fun fetchPolymarket(topic: String, limit: Int): JsonArray {
        return runCatching {
            val encoded = Uri.encode(topic)
            val body = httpClient.get("https://gamma-api.polymarket.com/public-search?q=$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val events = root["events"]?.jsonArray ?: return buildJsonArray {}
            buildJsonArray {
                events.take(limit).forEach { el ->
                    val ev = el.jsonObject
                    val slug = ev["slug"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    add(buildJsonObject {
                        put("title", ev["title"]?.jsonPrimitive?.contentOrNull.orEmpty().take(160))
                        put("volume", ev["volume"]?.jsonPrimitive?.contentOrNull ?: "0")
                        put("url", if (slug.isNotBlank()) "https://polymarket.com/event/$slug" else "https://polymarket.com")
                    })
                }
            }
        }.getOrElse {
            buildJsonArray { add(buildJsonObject { put("error", "Polymarket fetch failed: ${it.message}") }) }
        }
    }

    private suspend fun fetchWebLite(topic: String, limit: Int, maxChars: Int): JsonArray {
        return runCatching {
            val encoded = Uri.encode(topic)
            val html = httpClient.get("https://html.duckduckgo.com/html/?q=$encoded") {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 16; YLP-W00) AppleWebKit/537.36 Chrome/134.0.0.0 Safari/537.36")
            }.bodyAsText()
            val linkRegex = Regex("""<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val matches = linkRegex.findAll(html).take(limit).toList()
            buildJsonArray {
                matches.forEach { m ->
                    val rawUrl = m.groupValues[1]
                    val title = m.groupValues[2].replace("<[^>]*>".toRegex(), "").trim().take(maxChars)
                    add(buildJsonObject {
                        put("title", title)
                        put("url", rawUrl)
                    })
                }
            }
        }.getOrElse {
            buildJsonArray { add(buildJsonObject { put("error", "Web search failed: ${it.message}") }) }
        }
    }

    private suspend fun fetchXViaWebView(topic: String, limit: Int, maxChars: Int): JsonArray {
        return runCatching {
            val aiSpace = SpaceManager.resolveAiSpace(null)
            val encoded = Uri.encode(topic)
            SpaceManager.navigateAndWait(aiSpace.id, "https://x.com/search?q=$encoded&f=top", waitMs = 3200L)
            val js = """
                (function() {
                    const articles = Array.from(document.querySelectorAll('article')).slice(0, $limit);
                    return JSON.stringify(articles.map(a => ({
                        text: (a.innerText || '').replace(/\s+/g, ' ').trim().slice(0, $maxChars)
                    })));
                })();
            """.trimIndent()
            val raw = SpaceManager.evaluateJsInSpace(aiSpace.id, js)
            json.parseToJsonElement(raw).jsonArray
        }.getOrElse {
            buildJsonArray { add(buildJsonObject { put("note", "X WebView extraction requires logged-in session in Personal Space") }) }
        }
    }

    /**
     * 무료 웹 번역 엔드포인트 (브라우저 내장 자동 번역 버튼용)
     */
    suspend fun translateBatchToKorean(texts: List<String>): List<String> {
        if (texts.isEmpty()) return emptyList()
        val joined = texts.joinToString("\n|||EGO|||\n")
        return runCatching {
            val encoded = Uri.encode(joined)
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=ko&dt=t&q=$encoded"
            val body = httpClient.get(url).bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray
            val sentences = root[0].jsonArray
            val translatedFull = buildString {
                sentences.forEach { s ->
                    append(s.jsonArray[0].jsonPrimitive.contentOrNull.orEmpty())
                }
            }
            val split = translatedFull.split("|||EGO|||").map { it.trim() }
            if (split.size == texts.size) split else texts
        }.getOrElse { texts }
    }
}
