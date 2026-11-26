package com.egolite.mobile

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.app.UiModeManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.util.Rational
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.DownloadCenter
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.account.GoogleAccountManager
import com.egolite.mobile.model.ThemeMode
import com.egolite.mobile.model.WebFontMode
import com.egolite.mobile.ui.BrowserScreen
import com.egolite.mobile.ui.EgoLiteTheme
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_PIP_CONTROL = "com.egolite.mobile.ACTION_PIP_CONTROL"
        private const val EXTRA_PIP_CMD = "cmd"
    }

    private val pipControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_PIP_CONTROL) return
            val cmd = intent.getStringExtra(EXTRA_PIP_CMD) ?: return
            val js = when (cmd) {
                "toggle" -> "(function(){var v=document.querySelector('video');if(v){if(v.paused)v.play();else v.pause();}})();"
                "rew" -> "(function(){var v=document.querySelector('video');if(v){v.currentTime=Math.max(0,v.currentTime-10);}})();"
                "fwd" -> "(function(){var v=document.querySelector('video');if(v){v.currentTime=v.currentTime+10;}})();"
                else -> return
            }
            val activeSpace = SpaceManager.getActiveSpace()
            val wv = SpaceManager.getExistingWebView(activeSpace.activeTab.id)
            wv?.post { wv.evaluateJavascript(js, null) }
        }
    }

    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        val actions = listOf(
            createPipAction(101, "rew", android.R.drawable.ic_media_rew, "10초 뒤로"),
            createPipAction(102, "toggle", android.R.drawable.ic_media_play, "재생/일시정지"),
            createPipAction(103, "fwd", android.R.drawable.ic_media_ff, "10초 앞으로")
        )
        builder.setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun createPipAction(reqCode: Int, cmd: String, iconRes: Int, title: String): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).setPackage(packageName).putExtra(EXTRA_PIP_CMD, cmd)
        val pi = PendingIntent.getBroadcast(
            this,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pi)
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val cb = BrowserActivityBridge.filePathCallback
        BrowserActivityBridge.filePathCallback = null
        cb?.onReceiveValue(if (uris.isNullOrEmpty()) null else uris.toTypedArray())
    }

    private val bookmarkImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val html = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val parsed = BrowserActivityBridge.parseNetscapeBookmarksHtml(html)
                val added = SpaceManager.importBookmarks(parsed)
                Toast.makeText(this, "북마크 ${added}개 가져오기 완료", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "북마크 가져오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val bookmarkExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val html = BrowserActivityBridge.exportNetscapeBookmarksHtml(SpaceManager.bookmarks.value)
                contentResolver.openOutputStream(uri)?.use { it.write(html.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(this, "북마크 HTML 내보내기 완료", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "북마크 내보내기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val fontPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                var displayName = "custom_font.ttf"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(idx) ?: displayName
                    }
                }
                val fontDir = File(filesDir, "custom_fonts").apply { mkdirs() }
                val ext = if (displayName.endsWith(".otf", true)) "otf" else "ttf"
                val dest = File(fontDir, "user_custom_font.$ext")
                contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                SpaceManager.updatePreferences {
                    it.copy(
                        webFontMode = WebFontMode.CUSTOM_TTF,
                        customFontPath = dest.absolutePath,
                        customFontName = displayName
                    )
                }
                SpaceManager.reload(SpaceManager.getActiveSpace().id)
                Toast.makeText(this, "커스텀 폰트 적용됨: $displayName", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "폰트 파일 불러오기 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.install(applicationContext)
        ContextCompat.registerReceiver(
            this,
            pipControlReceiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        WindowCompat.setDecorFitsSystemWindows(window, true)
        SpaceManager.init(applicationContext)
        GoogleAccountManager.init(applicationContext)
        DownloadCenter.init(applicationContext)
        requestNotificationPermissionIfNeeded()

        BrowserActivityBridge.launchFilePicker = { mimeTypes -> filePickerLauncher.launch(mimeTypes) }
        BrowserActivityBridge.launchBookmarkImport = { bookmarkImportLauncher.launch(arrayOf("text/html", "*/*")) }
        BrowserActivityBridge.launchBookmarkExport = { bookmarkExportLauncher.launch("ego-lite-bookmarks.html") }
        BrowserActivityBridge.launchFontPicker = { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "*/*")) }

        handleIncomingBrowserIntent(intent)

        var lastExitBackPressedMs = 0L
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (BrowserActivityBridge.fullscreenVideo.value.view != null) {
                    BrowserActivityBridge.exitFullscreen()
                    return
                }
                val active = SpaceManager.getActiveSpace()
                if (SpaceManager.handleBackOrCloseTab(active.id)) {
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastExitBackPressedMs <= 2000L) {
                    finish()
                } else {
                    lastExitBackPressedMs = now
                    Toast.makeText(this@MainActivity, "한 번 더 뒤로가기를 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
                }
            }
        })

        setContent {
            val prefs by SpaceManager.preferences.collectAsState()
            val fullscreenVideo by BrowserActivityBridge.fullscreenVideo.collectAsState()
            val systemDark = isSystemInDarkTheme()
            LaunchedEffect(fullscreenVideo.view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { setPictureInPictureParams(buildPipParams(autoEnter = fullscreenVideo.view != null)) }
                }
            }
            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LaunchedEffect(prefs.themeMode, isDark, prefs.darkenWebsitesWhenPossible) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching {
                        val uiModeManager = getSystemService(UiModeManager::class.java)
                        val mode = when (prefs.themeMode) {
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                        }
                        uiModeManager?.setApplicationNightMode(mode)
                    }
                }
                SpaceManager.syncWebViewsDarkMode(isDark)
            }
            SideEffect {
                val isLight = !isDark
                val barColor = if (isLight) Color.parseColor("#F1F5F9") else Color.parseColor("#0F172A")
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
            EgoLiteTheme(darkTheme = isDark) {
                BrowserScreen()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (BrowserActivityBridge.fullscreenVideo.value.view != null && !isInPictureInPictureMode) {
            runCatching { enterPictureInPictureMode(buildPipParams(autoEnter = true)) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        BrowserActivityBridge.setInPictureInPictureMode(isInPictureInPictureMode)
    }

    override fun onResume() {
        super.onResume()
        SpaceManager.resumeActiveWebView()
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            SpaceManager.pauseIdleWebViewsForBackground()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingBrowserIntent(intent)
    }

    private fun handleIncomingBrowserIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val raw = intent.dataString ?: return
            val resolvedUrl = when {
                raw.startsWith("http://") || raw.startsWith("https://") -> raw
                raw.startsWith("intent://") -> runCatching {
                    Intent.parseUri(raw, Intent.URI_INTENT_SCHEME).getStringExtra("browser_fallback_url")
                }.getOrNull()
                else -> null
            }
            if (!resolvedUrl.isNullOrBlank() && (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://"))) {
                SpaceManager.openExternalUrl(resolvedUrl)
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipControlReceiver) }
        BrowserActivityBridge.launchFilePicker = null
        BrowserActivityBridge.launchBookmarkImport = null
        BrowserActivityBridge.launchBookmarkExport = null
        BrowserActivityBridge.launchFontPicker = null
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
