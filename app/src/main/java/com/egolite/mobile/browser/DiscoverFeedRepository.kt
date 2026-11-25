package com.egolite.mobile.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class NewsArticleItem(
    val title: String,
    val url: String,
    val source: String,
    val pubDate: String
)

data class TrendIdeaItem(
    val title: String,
    val url: String,
    val source: String,
    val score: Int,
    val comments: Int,
    val normalizedHeat: Float
)

enum class NewsCategory(val label: String, val query: String?) {
    TOP("주요 뉴스", null),
    TECH("IT · AI", "인공지능 OR 스마트폰 OR 테크"),
    ECONOMY("경제 · 증시", "경제 OR 반도체 OR 증시"),
    HOT("인기 이슈", "트렌드 OR 신제품")
}

object DiscoverFeedRepository {
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val UA =
        "Mozilla/5.0 (Linux; Android 16; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36"

    private val newsCache = ConcurrentHashMap<NewsCategory, Pair<Long, List<NewsArticleItem>>>()
    private var trendsCache: Pair<Long, List<TrendIdeaItem>>? = null

    private fun httpGet(endpoint: String, timeoutMs: Int = 8_000): String? {
        return runCatching {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/rss+xml, application/xml, application/json, text/xml, */*")
            }
            try {
                if (conn.responseCode !in 200..299) return null
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun fetchKoreaNews(category: NewsCategory, forceRefresh: Boolean = false): List<NewsArticleItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            newsCache[category]?.let { (ts, list) ->
                if (now - ts < CACHE_TTL_MS && list.isNotEmpty()) return list
            }
        }
        return withContext(Dispatchers.IO) {
            val rssUrl = if (category.query == null) {
                "https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko"
            } else {
                "https://news.google.com/rss/search?q=${URLEncoder.encode(category.query, "UTF-8")}&hl=ko&gl=KR&ceid=KR:ko"
            }
            val xml = httpGet(rssUrl) ?: return@withContext emptyList()
            val parsed = parseGoogleNewsRss(xml).take(12)
            if (parsed.isNotEmpty()) {
                newsCache[category] = System.currentTimeMillis() to parsed
            }
            parsed
        }
    }

    private fun parseGoogleNewsRss(xml: String): List<NewsArticleItem> {
        val items = mutableListOf<NewsArticleItem>()
        runCatching {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false
            var title = ""
            var link = ""
            var source = ""
            var pubDate = ""
            var currentTag = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.orEmpty()
                        if (currentTag == "item") {
                            inItem = true
                            title = ""
                            link = ""
                            source = ""
                            pubDate = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            when (currentTag) {
                                "title" -> title += parser.text.orEmpty()
                                "link" -> link += parser.text.orEmpty()
                                "source" -> source += parser.text.orEmpty()
                                "pubDate" -> pubDate += parser.text.orEmpty()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            val cleanTitle = title.trim()
                            val splitIdx = cleanTitle.lastIndexOf(" - ")
                            val finalTitle = if (splitIdx > 6) cleanTitle.substring(0, splitIdx).trim() else cleanTitle
                            val finalSource = source.trim().ifBlank {
                                if (splitIdx > 6) cleanTitle.substring(splitIdx + 3).trim() else "Google 뉴스"
                            }
                            if (finalTitle.isNotBlank() && link.trim().startsWith("http")) {
                                items.add(
                                    NewsArticleItem(
                                        title = finalTitle,
                                        url = link.trim(),
                                        source = finalSource,
                                        pubDate = pubDate.trim().take(16)
                                    )
                                )
                            }
                            inItem = false
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
        }
        return items
    }

    /**
     * Last30Days 스타일 글로벌 트렌드 & 아이디어 피드 (Hacker News + Reddit AI/Tech 실시간 수집)
     */
    suspend fun fetchLast30DaysTrends(forceRefresh: Boolean = false): List<TrendIdeaItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            trendsCache?.let { (ts, list) ->
                if (now - ts < CACHE_TTL_MS && list.isNotEmpty()) return list
            }
        }
        return withContext(Dispatchers.IO) {
            val rawItems = mutableListOf<TrendIdeaItem>()
            coroutineScope {
                val hnTask = async { fetchHackerNewsTop(8) }
                val redditTask = async { fetchRedditHot("artificial", 6) }
                rawItems.addAll(hnTask.await())
                rawItems.addAll(redditTask.await())
            }
            val sorted = rawItems.sortedByDescending { it.score + it.comments * 2 }.take(12)
            val maxMetric = (sorted.maxOfOrNull { it.score + it.comments * 2 } ?: 1).coerceAtLeast(1)
            val normalized = sorted.map { item ->
                val heat = ((item.score + item.comments * 2).toFloat() / maxMetric.toFloat()).coerceIn(0.15f, 1f)
                item.copy(normalizedHeat = heat)
            }
            if (normalized.isNotEmpty()) {
                trendsCache = System.currentTimeMillis() to normalized
            }
            normalized
        }
    }

    private suspend fun fetchHackerNewsTop(limit: Int): List<TrendIdeaItem> = coroutineScope {
        val idsRaw = httpGet("https://hacker-news.firebaseio.com/v0/topstories.json") ?: return@coroutineScope emptyList()
        val ids = runCatching {
            val arr = JSONArray(idsRaw)
            (0 until minOf(limit, arr.length())).map { arr.getLong(it) }
        }.getOrDefault(emptyList())

        ids.map { id ->
            async(Dispatchers.IO) {
                val raw = httpGet("https://hacker-news.firebaseio.com/v0/item/$id.json", 5_000) ?: return@async null
                runCatching {
                    val obj = JSONObject(raw)
                    val title = obj.optString("title", "").trim()
                    val url = obj.optString("url", "").ifBlank { "https://news.ycombinator.com/item?id=$id" }
                    val score = obj.optInt("score", 0)
                    val comments = obj.optInt("descendants", 0)
                    if (title.isBlank()) null else TrendIdeaItem(title, url, "Hacker News", score, comments, 0.5f)
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private fun fetchRedditHot(subreddit: String, limit: Int): List<TrendIdeaItem> {
        val raw = httpGet("https://www.reddit.com/r/$subreddit/hot.json?limit=$limit", 6_000) ?: return emptyList()
        return runCatching {
            val children = JSONObject(raw).getJSONObject("data").getJSONArray("children")
            (0 until children.length()).mapNotNull { i ->
                val d = children.getJSONObject(i).getJSONObject("data")
                if (d.optBoolean("stickied", false)) return@mapNotNull null
                val title = d.optString("title", "").trim()
                val permalink = d.optString("permalink", "")
                val url = if (permalink.isNotBlank()) "https://www.reddit.com$permalink" else d.optString("url", "")
                val score = d.optInt("ups", 0)
                val comments = d.optInt("num_comments", 0)
                if (title.isBlank() || url.isBlank()) null
                else TrendIdeaItem(title, url, "r/$subreddit", score, comments, 0.5f)
            }
        }.getOrDefault(emptyList())
    }
}
