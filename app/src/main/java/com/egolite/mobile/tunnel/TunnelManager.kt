package com.egolite.mobile.tunnel

import android.content.Context
import android.net.ConnectivityManager
import com.egolite.mobile.model.TunnelProvider
import com.egolite.mobile.model.TunnelSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Inet4Address

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
            onError("터널 바이너리($binaryName)를 찾을 수 없습니다. Codemagic으로 빌드했는지 확인하세요.")
            return
        }

        val resolvFile = prepareAndroidResolvConf()
        val cmd = buildCommand(binFile.absolutePath, settings)
        onLog("터널 시작: ${settings.provider.name}")

        readerJob = scope.launch(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectInput(resolvFile)
                    .directory(context.filesDir)
                pb.environment()["HOME"] = context.filesDir.absolutePath
                // Android 비루팅 환경에서 패치된 바이너리(//proc/self/fd/0)가 pure-Go DNS 리졸버로 동작하도록 강제
                pb.environment()["GODEBUG"] = "netdns=go"
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

    /**
     * Android에는 /etc/resolv.conf가 없어 Go 바이너리가 [::1]:53으로 질의하다 실패한다.
     * 빌드 시 /etc/resolv.conf -> //proc/self/fd/0 으로 패치된 바이너리에
     * 실제 기기 DNS + 공용 DNS(1.1.1.1, 8.8.8.8)를 담은 파일을 stdin(fd 0)으로 연결한다.
     */
    private fun prepareAndroidResolvConf(): File {
        val dnsList = linkedSetOf<String>()
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val linkProps = if (activeNet != null) cm.getLinkProperties(activeNet) else null
            linkProps?.dnsServers?.forEach { addr ->
                val host = addr.hostAddress?.substringBefore("%")
                if (!host.isNullOrBlank() && addr is Inet4Address && host != "127.0.0.1") {
                    dnsList.add(host)
                }
            }
        }
        dnsList.add("1.1.1.1")
        dnsList.add("8.8.8.8")
        dnsList.add("1.0.0.1")

        val content = buildString {
            dnsList.forEach { ns -> append("nameserver ").append(ns).append('\n') }
            append("options ndots:0 timeout:3 attempts:2\n")
        }
        val file = File(context.filesDir, "resolv.conf")
        file.writeText(content)
        onLog("Android DNS 설정 적용: ${dnsList.joinToString(", ")}")
        return file
    }

    private fun buildCommand(binPath: String, settings: TunnelSettings): List<String> {
        return when (settings.provider) {
            TunnelProvider.CLOUDFLARE -> {
                if (settings.cloudflareToken.isNotBlank()) {
                    listOf(
                        binPath,
                        "tunnel",
                        "--no-autoupdate",
                        "--edge-ip-version", "4",
                        "run",
                        "--token", settings.cloudflareToken.trim()
                    )
                } else {
                    listOf(
                        binPath,
                        "tunnel",
                        "--no-autoupdate",
                        "--edge-ip-version", "4",
                        "--url", "http://127.0.0.1:${settings.localPort}"
                    )
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
