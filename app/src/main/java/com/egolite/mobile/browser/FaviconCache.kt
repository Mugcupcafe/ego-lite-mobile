package com.egolite.mobile.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object FaviconCache {
    private val memoryCache = ConcurrentHashMap<String, Bitmap>()
    private val httpClient = HttpClient(CIO) {
        engine { requestTimeout = 8_000 }
    }

    private fun extractHost(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return runCatching { Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")
    }

    fun getMemoryCached(url: String?): Bitmap? {
        val host = extractHost(url)
        return if (host.isNotBlank()) memoryCache[host] else null
    }

    fun putIcon(context: Context, url: String?, bitmap: Bitmap?) {
        val host = extractHost(url)
        if (host.isBlank() || bitmap == null) return
        memoryCache[host] = bitmap
        runCatching {
            val dir = File(context.cacheDir, "favicons")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${host.replace(Regex("[^a-z0-9._-]"), "_")}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }
    }

    suspend fun loadFavicon(context: Context, url: String?): Bitmap? {
        val host = extractHost(url)
        if (host.isBlank()) return null
        memoryCache[host]?.let { return it }

        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "favicons")
            val safeName = host.replace(Regex("[^a-z0-9._-]"), "_")
            val file = File(dir, "$safeName.png")
            if (file.exists()) {
                val decoded = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                if (decoded != null) {
                    memoryCache[host] = decoded
                    return@withContext decoded
                }
            }

            val fetched = runCatching {
                val resp = httpClient.get("https://www.google.com/s2/favicons?domain=${Uri.encode(host)}&sz=64")
                if (resp.status.value in 200..299) {
                    val bytes = resp.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null
            }.getOrNull()

            if (fetched != null) {
                memoryCache[host] = fetched
                runCatching {
                    if (!dir.exists()) dir.mkdirs()
                    FileOutputStream(file).use { out ->
                        fetched.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                }
            }
            fetched
        }
    }
}
