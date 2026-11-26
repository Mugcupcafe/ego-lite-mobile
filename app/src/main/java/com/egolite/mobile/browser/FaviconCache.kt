package com.egolite.mobile.browser

import android.util.LruCache
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object FaviconCache {
    // 메모리 최적화: 최대 120개 파비콘만 RAM에 유지 (초과 시 자동 LRU 정리)
    private val memoryCache = LruCache<String, Bitmap>(120)
    private val failedHosts = ConcurrentHashMap<String, Long>()

    private const val UA =
        "Mozilla/5.0 (Linux; Android 16; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36"

    private fun extractHost(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return runCatching { Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")
    }

    private fun rootDomainOf(host: String): String {
        return host.removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
    }

    fun getMemoryCached(url: String?): Bitmap? {
        val host = extractHost(url)
        if (host.isBlank()) return null
        return memoryCache.get(host) ?: memoryCache.get(rootDomainOf(host))
    }

    fun putIcon(context: Context, url: String?, bitmap: Bitmap?) {
        val host = extractHost(url)
        if (host.isBlank() || bitmap == null || bitmap.width < 8 || bitmap.height < 8) return
        memoryCache.put(host, bitmap)
        failedHosts.remove(host)
        saveToDisk(context, host, bitmap)
    }

    private fun saveToDisk(context: Context, host: String, bitmap: Bitmap) {
        runCatching {
            val dir = File(context.cacheDir, "favicons")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${host.replace(Regex("[^a-z0-9._-]"), "_")}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
        }
    }

    private fun downloadBitmap(endpoint: String): Bitmap? {
        return runCatching {
            var currentUrl = endpoint
            var redirects = 0
            while (redirects < 4) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 6_000
                    readTimeout = 7_000
                    setRequestProperty("User-Agent", UA)
                    setRequestProperty("Accept", "image/png,image/webp,image/x-icon,image/*,*/*;q=0.8")
                }
                try {
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location") ?: return null
                        currentUrl = URL(URL(currentUrl), loc).toString()
                        redirects++
                        continue
                    }
                    if (code !in 200..299) return null
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.size < 80) return null
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null && bmp.width >= 16 && bmp.height >= 16) {
                        return bmp
                    }
                    return null
                } finally {
                    conn.disconnect()
                }
            }
            null
        }.getOrNull()
    }

    suspend fun loadFavicon(context: Context, url: String?): Bitmap? {
        val host = extractHost(url)
        if (host.isBlank()) return null
        memoryCache.get(host)?.let { return it }
        val root = rootDomainOf(host)
        memoryCache.get(root)?.let { return it }

        // 최근 2분 이내 실패한 호스트는 과도한 재요청 방지
        val lastFail = failedHosts[host]
        if (lastFail != null && System.currentTimeMillis() - lastFail < 120_000L) {
            return null
        }

        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "favicons")
            for (candidateHost in listOf(host, root).distinct()) {
                val safeName = candidateHost.replace(Regex("[^a-z0-9._-]"), "_")
                val file = File(dir, "$safeName.png")
                if (file.exists() && file.length() > 80L) {
                    val decoded = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                    if (decoded != null && decoded.width >= 16) {
                        memoryCache.put(host, decoded)
                        return@withContext decoded
                    }
                }
            }

            // 4단계 폴백 체인: Google 고해상도(128px) -> DuckDuckGo -> 사이트 직접 아이콘 -> 루트 도메인
            val candidates = buildList {
                add("https://www.google.com/s2/favicons?domain_url=https://${Uri.encode(host)}&sz=128")
                add("https://icons.duckduckgo.com/ip3/${Uri.encode(host)}.ico")
                add("https://$host/apple-touch-icon.png")
                add("https://$host/favicon.ico")
                if (root != host && root.contains(".")) {
                    add("https://www.google.com/s2/favicons?domain_url=https://${Uri.encode(root)}&sz=128")
                    add("https://icons.duckduckgo.com/ip3/${Uri.encode(root)}.ico")
                }
            }

            var fetched: Bitmap? = null
            for (endpoint in candidates) {
                val bmp = downloadBitmap(endpoint)
                if (bmp != null) {
                    fetched = bmp
                    break
                }
            }

            if (fetched != null) {
                memoryCache.put(host, fetched)
                failedHosts.remove(host)
                saveToDisk(context, host, fetched)
            } else {
                failedHosts[host] = System.currentTimeMillis()
            }
            fetched
        }
    }
}
