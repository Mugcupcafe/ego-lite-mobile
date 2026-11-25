package com.egolite.mobile

import android.Manifest
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
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.ThemeMode
import com.egolite.mobile.model.WebFontMode
import com.egolite.mobile.ui.BrowserScreen
import com.egolite.mobile.ui.EgoLiteTheme
import java.io.File

class MainActivity : ComponentActivity() {

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
        WindowCompat.setDecorFitsSystemWindows(window, true)
        SpaceManager.init(applicationContext)
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
            val systemDark = isSystemInDarkTheme()
            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            LaunchedEffect(isDark) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingBrowserIntent(intent)
    }

    private fun handleIncomingBrowserIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                SpaceManager.openExternalUrl(url)
            }
        }
    }

    override fun onDestroy() {
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
