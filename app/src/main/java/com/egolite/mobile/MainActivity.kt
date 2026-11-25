package com.egolite.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.egolite.mobile.browser.BrowserActivityBridge
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.ui.BrowserScreen
import com.egolite.mobile.ui.EgoLiteTheme

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

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.install(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        SpaceManager.init(applicationContext)
        requestNotificationPermissionIfNeeded()

        BrowserActivityBridge.launchFilePicker = { mimeTypes ->
            filePickerLauncher.launch(mimeTypes)
        }
        BrowserActivityBridge.launchBookmarkImport = {
            bookmarkImportLauncher.launch(arrayOf("text/html", "*/*"))
        }
        BrowserActivityBridge.launchBookmarkExport = {
            bookmarkExportLauncher.launch("ego-lite-bookmarks.html")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (BrowserActivityBridge.fullscreenVideo.value.view != null) {
                    BrowserActivityBridge.exitFullscreen()
                    return
                }
                val active = SpaceManager.getActiveSpace()
                if (!SpaceManager.handleBackOrCloseTab(active.id)) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            val prefs by SpaceManager.preferences.collectAsState()
            SideEffect {
                val isLight = !prefs.darkModeEnabled
                val barColor = if (isLight) Color.parseColor("#F1F5F9") else Color.parseColor("#0F172A")
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
            }
            EgoLiteTheme(darkTheme = prefs.darkModeEnabled) {
                BrowserScreen()
            }
        }
    }

    override fun onDestroy() {
        BrowserActivityBridge.launchFilePicker = null
        BrowserActivityBridge.launchBookmarkImport = null
        BrowserActivityBridge.launchBookmarkExport = null
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
