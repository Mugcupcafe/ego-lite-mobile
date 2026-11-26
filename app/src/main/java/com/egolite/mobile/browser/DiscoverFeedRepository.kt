package com.egolite.mobile.browser

import android.util.LruCache
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class NewsArticleItem(
    val title: String,
    val url: String,
    val source: String,
    val sourceUrl: String,
    val pubDate: String,
    val relativeTime: String,
    val imageUrl: String = ""
)

data class TrendIdeaItem(
    val title: String,
    val url: String,
    val source: String,
    val sourceUrl: String,
    val score: Int,
    val comments: Int,
    val normalizedHeat: Float
)

enum class NewsCategory(val label: String) {
    TOP("주요 뉴스"),
    TECH("IT · AI"),
    ECONOMY("경제 · 증시"),
    HOT("인기 이슈")
}

object DiscoverFeedRepository {
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val UA =
        "Mozilla/5.0 (Linux; Android 16; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36"

    private val newsCache = ConcurrentHashMap<NewsCategory, Pair<Long, List<NewsArticleItem>>>()
    private var trendsCache: Pair<Long, List<TrendIdeaItem>>? = null
    // 메모리 최적화: Discover 피드 썸네일 최대 40장만 RAM에 유지 (LRU 자동 정리)
    private val imageCache = LruCache<String, Bitmap>(40)
    private val failedImages = ConcurrentHashMap<String, Long>()

    private fun httpGet(endpoint: String, timeoutMs: Int = 7_500, maxBytes: Int = 512_000): String? {
        return runCatching {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/rss+xml, application/xml, application/json, text/html, */*")
            }
            try {
                if (conn.responseCode !in 200..299) return null
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    val sb = StringBuilder()
                    val buf = CharArray(4096)
                    var total = 0
                    while (total < maxBytes) {
                        val read = reader.read(buf)
                        if (read <= 0) break
                        sb.append(buf, 0, read)
                        total += read
                    }
                    sb.toString()
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun loadArticleImage(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank() || !imageUrl.startsWith("http")) return null
        imageCache.get(imageUrl)?.let { return it }
        val lastFail = failedImages[imageUrl]
        if (lastFail != null && System.currentTimeMillis() - lastFail < 180_000L) return null

        return withContext(Dispatchers.IO) {
            val bmp = runCatching {
                val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 6_000
                    readTimeout = 7_000
                    setRequestProperty("User-Agent", UA)
                    setRequestProperty("Accept", "image/webp,image/jpeg,image/png,image/*,*/*;q=0.8")
                }
                try {
                    if (conn.responseCode !in 200..299) return@runCatching null
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.size < 200) return@runCatching null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (bounds.outWidth / sample > 720) sample *= 2
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()

            if (bmp != null) {
                if (imageCache.size > 40) imageCache.clear()
                imageCache.put(imageUrl, bmp)
                failedImages.remove(imageUrl)
            } else {
                failedImages[imageUrl] = System.currentTimeMillis()
            }
            bmp
        }
    }

    suspend fun fetchKoreaNews(category: NewsCategory, forceRefresh: Boolean = false): List<NewsArticleItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            newsCache[category]?.let { (ts, list) ->
                if (now - ts < CACHE_TTL_MS && list.isNotEmpty()) return list
            }
        }
        return withContext(Dispatchers.IO) {
            val feeds: List<Pair<String, String>> = when (category) {
                NewsCategory.TOP -> listOf(
                    "SBS 뉴스" to "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=01&plink=RSSREADER",
                    "연합뉴스" to "https://www.yna.co.kr/rss/news.xml",
                    "GeekNews" to "https://news.hada.io/rss/news"
                )
                NewsCategory.TECH -> listOf(
                    "GeekNews" to "https://news.hada.io/rss/news",
                    "ZDNet Korea" to "https://feeds.feedburner.com/zdkorea",
                    "SBS IT" to "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=08&plink=RSSREADER"
                )
                NewsCategory.ECONOMY -> listOf(
                    "SBS 경제" to "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=02&plink=RSSREADER",
                    "매일경제" to "https://www.mk.co.kr/rss/30100041/"
                )
                NewsCategory.HOT -> listOf(
                    "SBS 문화" to "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=07&plink=RSSREADER",
                    "SBS 연예" to "https://news.sbs.co.kr/news/SectionRssFeed.do?sectionId=14&plink=RSSREADER",
                    "GeekNews" to "https://news.hada.io/rss/news"
                )
            }

            val combined = mutableListOf<NewsArticleItem>()
            coroutineScope {
                val results = feeds.map { (defaultSource, url) ->
                    async(Dispatchers.IO) {
                        val xml = httpGet(url) ?: return@async emptyList<NewsArticleItem>()
                        parseRssOrAtom(xml, defaultSource).take(8)
                    }
                }.awaitAll()

                // 언론사가 섞여 보이도록 라운드로빈 병합
                val maxLen = results.maxOfOrNull { it.size } ?: 0
                for (i in 0 until maxLen) {
                    for (list in results) {
                        list.getOrNull(i)?.let { combined.add(it) }
                    }
                }
            }

            // 사진이 없는 기사는 앞 10개에 한해 og:image 병렬 추출
            val topList = combined.distinctBy { it.title }.take(16)
            val enriched = coroutineScope {
                topList.mapIndexed { idx, item ->
                    async(Dispatchers.IO) {
                        if (item.imageUrl.isNotBlank() || idx >= 10 || item.url.contains("news.google.com")) {
                            item
                        } else {
                            val og = fetchOgImage(item.url)
                            if (!og.isNullOrBlank()) item.copy(imageUrl = og) else item
                        }
                    }
                }.awaitAll()
            }

            // 위 피드가 모두 비어있을 때만 Google News RSS 폴백
            val finalItems = if (enriched.isNotEmpty()) {
                enriched
            } else {
                val fallbackUrl = "https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko"
                val xml = httpGet(fallbackUrl)
                if (xml != null) parseRssOrAtom(xml, "Google 뉴스").take(14) else emptyList()
            }

            if (finalItems.isNotEmpty()) {
                newsCache[category] = System.currentTimeMillis() to finalItems
            }
            finalItems
        }
    }

    private fun fetchOgImage(articleUrl: String): String? {
        val html = httpGet(articleUrl, timeoutMs = 3_500, maxBytes = 28_000) ?: return null
        val patterns = listOf(
            Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val found = p.find(html)?.groupValues?.getOrNull(1)?.trim()
            if (!found.isNullOrBlank() && found.startsWith("http")) {
                return found.replace("&amp;", "&")
            }
        }
        return null
    }

    private fun parseRssOrAtom(xml: String, defaultSource: String): List<NewsArticleItem> {
        val items = mutableListOf<NewsArticleItem>()
        runCatching {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var inItem = false
            var title = ""
            var link = ""
            var source = ""
            var pubDate = ""
            var imageUrl = ""
            var description = ""
            var currentTag = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.orEmpty().lowercase()
                        currentTag = name
                        if (name == "item" || name == "entry") {
                            inItem = true
                            title = ""
                            link = ""
                            source = ""
                            pubDate = ""
                            imageUrl = ""
                            description = ""
                        } else if (inItem) {
                            if (name == "enclosure" || name == "media:content" || name == "media:thumbnail") {
                                val u = parser.getAttributeValue(null, "url").orEmpty().trim()
                                val type = parser.getAttributeValue(null, "type").orEmpty().lowercase()
                                if (u.startsWith("http") && (type.isBlank() || type.startsWith("image") || name.contains("media"))) {
                                    if (imageUrl.isBlank()) imageUrl = u
                                }
                            } else if (name == "link") {
                                val href = parser.getAttributeValue(null, "href").orEmpty().trim()
                                if (href.startsWith("http") && link.isBlank()) link = href
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            when (currentTag) {
                                "title" -> title += parser.text.orEmpty()
                                "link" -> if (link.isBlank()) link += parser.text.orEmpty()
                                "source" -> source += parser.text.orEmpty()
                                "pubdate", "published", "updated" -> pubDate += parser.text.orEmpty()
                                "description", "content", "summary" -> description += parser.text.orEmpty()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.orEmpty().lowercase()
                        if ((name == "item" || name == "entry") && inItem) {
                            val cleanTitle = title.replace(Regex("<[^>]+>"), "").trim()
                            val cleanLink = link.trim()
                            if (imageUrl.isBlank() && description.isNotBlank()) {
                                val imgMatch = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(description)
                                val cand = imgMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
                                if (cand.startsWith("http")) imageUrl = cand.replace("&amp;", "&")
                            }
                            val splitIdx = cleanTitle.lastIndexOf(" - ")
                            val finalTitle = if (defaultSource == "Google 뉴스" && splitIdx > 6) {
                                cleanTitle.substring(0, splitIdx).trim()
                            } else cleanTitle
                            val finalSource = source.trim().ifBlank {
                                if (defaultSource == "Google 뉴스" && splitIdx > 6) cleanTitle.substring(splitIdx + 3).trim()
                                else defaultSource
                            }
                            if (finalTitle.isNotBlank() && cleanLink.startsWith("http")) {
                                val hostUrl = runCatching {
                                    val u = URL(cleanLink)
                                    "${u.protocol}://${u.host}"
                                }.getOrDefault(cleanLink)
                                items.add(
                                    NewsArticleItem(
                                        title = finalTitle,
                                        url = cleanLink,
                                        source = finalSource,
                                        sourceUrl = hostUrl,
                                        pubDate = pubDate.trim(),
                                        relativeTime = formatRelativeKoreanTime(pubDate.trim()),
                                        imageUrl = imageUrl
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

    private fun formatRelativeKoreanTime(rawDate: String): String {
        if (rawDate.isBlank()) return "오늘"
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        var parsed: Date? = null
        for (fmt in formats) {
            parsed = runCatching { SimpleDateFormat(fmt, Locale.ENGLISH).parse(rawDate) }.getOrNull()
            if (parsed != null) break
        }
        if (parsed == null) return "오늘"
        val diffMin = ((System.currentTimeMillis() - parsed.time) / 60_000L).coerceAtLeast(0L)
        return when {
            diffMin < 2 -> "방금"
            diffMin < 60 -> "${diffMin}분"
            diffMin < 24 * 60 -> "${diffMin / 60}시간"
            else -> "${diffMin / (24 * 60)}일"
        }
    }

    /**
     * Last30Days 스타일 글로벌 트렌드 & 아이디어 피드 (Hacker News + Reddit r/artificial 실시간 수집)
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
                    if (title.isBlank()) null
                    else TrendIdeaItem(title, url, "Hacker News", "https://news.ycombinator.com", score, comments, 0.5f)
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
                else TrendIdeaItem(title, url, "r/$subreddit", "https://www.reddit.com", score, comments, 0.5f)
            }
        }.getOrDefault(emptyList())
    }
}
