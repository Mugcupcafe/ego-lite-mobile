package com.egolite.mobile.tunnel

import android.content.Context
import com.egolite.mobile.model.TunnelProvider
import com.egolite.mobile.model.TunnelSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TunnelManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPublicUrlReady: (String) -> Unit,
    private val onLog: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var process: Process? = null
    private var readerJob: Job? = null

    private val httpsUrlRegex = Regex("""https://[a-zA-Z0-9.-]+\.(trycloudflare\.com|ngrok-free\.app|ngrok\.app|ngrok\.io)""")

    fun start(settings: TunnelSettings) {
        stop()
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val binaryName = when (settings.provider) {
            TunnelProvider.CLOUDFLARE -> "libcloudflared.so"
            TunnelProvider.NGROK -> "libngrok.so"
        }
        val binFile = File(nativeDir, binaryName)
        if (!binFile.exists()) {
            onError("터널 바이너리($binaryName)를 찾을 수 없습니다. Codemagic/GitHub Actions로 빌드했는지 확인하세요.")
            return
        }

        val cmd = buildCommand(binFile.absolutePath, settings)
        onLog("터널 시작: ${settings.provider.name}")

        readerJob = scope.launch(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .directory(context.filesDir)
                pb.environment()["HOME"] = context.filesDir.absolutePath
                val proc = pb.start()
                process = proc

                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val current = line.orEmpty()
                        if (current.isNotBlank()) {
                            onLog(current.take(180))
                            val match = httpsUrlRegex.find(current)
                            if (match != null) {
                                onPublicUrlReady(match.value)
                            }
                        }
                    }
                }
                val code = proc.waitFor()
                onLog("터널 프로세스 종료 (code=$code)")
            } catch (e: Exception) {
                onError("터널 실행 오류: ${e.message}")
            }
        }
    }

    private fun buildCommand(binPath: String, settings: TunnelSettings): List<String> {
        return when (settings.provider) {
            TunnelProvider.CLOUDFLARE -> {
                if (settings.cloudflareToken.isNotBlank()) {
                    listOf(binPath, "tunnel", "--no-autoupdate", "run", "--token", settings.cloudflareToken.trim())
                } else {
                    listOf(binPath, "tunnel", "--no-autoupdate", "--url", "http://127.0.0.1:${settings.localPort}")
                }
            }
            TunnelProvider.NGROK -> {
                val args = mutableListOf(binPath, "http", "${settings.localPort}", "--log=stdout")
                if (settings.ngrokAuthToken.isNotBlank()) {
                    args += listOf("--authtoken", settings.ngrokAuthToken.trim())
                }
                if (settings.ngrokDomain.isNotBlank()) {
                    args += listOf("--domain", settings.ngrokDomain.trim())
                }
                args
            }
        }
    }

    fun stop() {
        readerJob?.cancel()
        readerJob = null
        process?.destroy()
        process = null
    }
}
