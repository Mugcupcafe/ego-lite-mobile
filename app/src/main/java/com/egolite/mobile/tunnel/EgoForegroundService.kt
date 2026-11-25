package com.egolite.mobile.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.egolite.mobile.MainActivity
import com.egolite.mobile.mcp.McpHttpServer
import com.egolite.mobile.mcp.OAuth21Manager
import com.egolite.mobile.model.McpConnectionState
import com.egolite.mobile.model.ServerRuntimeState
import com.egolite.mobile.model.TunnelSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EgoForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mcpServer: McpHttpServer? = null
    private var tunnelManager: TunnelManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        init(applicationContext)
        createNotificationChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EgoLiteMobile::McpServerLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("MCP 서버 및 HTTPS 터널 시작 중..."))
                startAll(currentTunnelSettings.value)
                return START_STICKY
            }
        }
    }

    private fun startAll(settings: TunnelSettings) {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(12 * 60 * 60 * 1000L)
        }
        if (settings.bearerToken.isNotBlank()) {
            OAuth21Manager.staticBearerToken = settings.bearerToken
        }
        _runtimeState.update {
            it.copy(
                connectionState = McpConnectionState.STARTING,
                localUrl = "http://127.0.0.1:${settings.localPort}/mcp",
                statusMessage = "MCP 서버 및 터널 시작 중..."
            )
        }

        mcpServer = McpHttpServer(settings.localPort) { appendLog(it) }.also { it.start() }

        tunnelManager = TunnelManager(
            context = this,
            scope = serviceScope,
            onPublicUrlReady = { httpsUrl ->
                OAuth21Manager.publicBaseUrl = httpsUrl
                val mcpUrl = "${httpsUrl.trimEnd('/')}/mcp"
                _runtimeState.update {
                    it.copy(
                        connectionState = McpConnectionState.CONNECTED,
                        publicHttpsUrl = mcpUrl,
                        statusMessage = "HTTPS 터널 연결됨"
                    )
                }
                updateNotification("MCP 연결됨: $mcpUrl")
            },
            onLog = { appendLog(it) },
            onError = { err ->
                appendLog(err)
                _runtimeState.update {
                    it.copy(
                        connectionState = McpConnectionState.ERROR,
                        statusMessage = err
                    )
                }
                updateNotification("MCP 터널 오류")
            }
        ).also { it.start(settings) }
    }

    private fun stopAll() {
        tunnelManager?.stop()
        mcpServer?.stop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        _runtimeState.update {
            it.copy(
                connectionState = McpConnectionState.DISCONNECTED,
                publicHttpsUrl = "",
                statusMessage = "MCP 서버 꺼짐"
            )
        }
    }

    override fun onDestroy() {
        stopAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ego Lite MCP Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ego Lite Mobile MCP")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(content))
    }

    companion object {
        private const val CHANNEL_ID = "ego_mcp_service_channel"
        private const val NOTIF_ID = 1001
        const val ACTION_START = "com.egolite.mobile.action.START"
        const val ACTION_STOP = "com.egolite.mobile.action.STOP"

        private val jsonCodec = Json { ignoreUnknownKeys = true }
        private lateinit var appContext: Context

        private val _tunnelSettings = MutableStateFlow(
            TunnelSettings(bearerToken = OAuth21Manager.staticBearerToken)
        )
        val currentTunnelSettings: StateFlow<TunnelSettings> = _tunnelSettings.asStateFlow()

        private val _runtimeState = MutableStateFlow(ServerRuntimeState())
        val runtimeState: StateFlow<ServerRuntimeState> = _runtimeState.asStateFlow()

        fun init(context: Context) {
            if (!::appContext.isInitialized) {
                appContext = context.applicationContext
                val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
                val raw = sp.getString("tunnel_settings_json", null)
                if (raw != null) {
                    runCatching { jsonCodec.decodeFromString<TunnelSettings>(raw) }.getOrNull()?.let { loaded ->
                        val effectiveToken = loaded.bearerToken.ifBlank { OAuth21Manager.staticBearerToken }
                        OAuth21Manager.staticBearerToken = effectiveToken
                        _tunnelSettings.value = loaded.copy(bearerToken = effectiveToken)
                    }
                } else {
                    saveTunnelSettings(_tunnelSettings.value)
                }
            }
        }

        private fun saveTunnelSettings(settings: TunnelSettings) {
            if (!::appContext.isInitialized) return
            val sp = appContext.getSharedPreferences("ego_lite_prefs", Context.MODE_PRIVATE)
            sp.edit().putString("tunnel_settings_json", jsonCodec.encodeToString(settings)).apply()
        }

        fun updateTunnelSettings(transform: (TunnelSettings) -> TunnelSettings) {
            val updated = transform(_tunnelSettings.value)
            _tunnelSettings.value = updated
            if (updated.bearerToken.isNotBlank()) {
                OAuth21Manager.staticBearerToken = updated.bearerToken
            }
            saveTunnelSettings(updated)
        }

        private fun appendLog(line: String) {
            _runtimeState.update { state ->
                state.copy(recentLogs = (state.recentLogs + line).takeLast(40))
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, EgoForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EgoForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
