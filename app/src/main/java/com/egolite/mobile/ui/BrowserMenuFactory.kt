package com.egolite.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.egolite.mobile.R
import com.egolite.mobile.browser.BrowserScripts
import com.egolite.mobile.browser.DownloadCenter
import com.egolite.mobile.browser.DuplicateDownloadPrompt
import com.egolite.mobile.browser.ExtensionManager
import com.egolite.mobile.account.GoogleAccountManager
import com.egolite.mobile.browser.InstalledExtension
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.BrowserPreferences
import com.egolite.mobile.model.BrowserSpace
import com.egolite.mobile.model.BrowserTab
import com.egolite.mobile.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * BrowserScreen()의 JVM 64KB 메서드 크기 제한을 여유롭게 유지하기 위해
 * 크롬 메뉴 항목 빌더 및 전체화면 보조 오버레이 묶음을 분리한 헬퍼 파일.
 */
fun buildBrowserChromeMenuItems(
    context: Context,
    scope: CoroutineScope,
    activeSpace: BrowserSpace,
    activeTab: BrowserTab,
    prefs: BrowserPreferences,
    isMcpDisconnected: Boolean,
    isWebPage: Boolean,
    isEffectiveDark: Boolean,
    currentHost: String,
    recentlyClosedEmpty: Boolean,
    onShowMcpDrawer: () -> Unit,
    onShowAdBlockDialog: () -> Unit,
    onShowChromeSettings: () -> Unit,
    onShowBookmarkManager: () -> Unit,
    onShowFindBar: () -> Unit,
    onTriggerTranslation: (String, String) -> Unit,
    onSetReaderMarkdown: (String) -> Unit,
    activeExtPopupId: String? = null,
    onTapExtension: ((InstalledExtension) -> Unit)? = null,
    onLongPressExtension: ((InstalledExtension) -> Unit)? = null
): List<ChromeMenuItemSpec> = buildList {
    add(
        ChromeMenuItemSpec(
            title = "MCP · 터널",
            icon = if (isMcpDisconnected) Icons.Default.LinkOff else Icons.Default.Hub,
            key = "mcp",
            checked = !isMcpDisconnected,
            bold = !isMcpDisconnected,
            dividerAfter = true,
            onClick = onShowMcpDrawer
        )
    )
    add(ChromeMenuItemSpec("새 탭", Icons.Default.AddBox, key = "newtab") { SpaceManager.createTab(activeSpace.id) })
    add(ChromeMenuItemSpec("AI 공간", Icons.Default.SmartToy, key = "aispace", accentColor = EgoEmerald) {
        SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) }
    })
    add(ChromeMenuItemSpec("시크릿 창", Icons.Default.VisibilityOff, key = "incognito", dividerAfter = true) {
        SpaceManager.createTab(activeSpace.id, incognito = true)
    })
    add(ChromeMenuItemSpec("방문 기록", Icons.Default.History, key = "history") { SpaceManager.openHistoryTab(activeSpace.id) })
    add(ChromeMenuItemSpec("기록 삭제", Icons.Default.DeleteOutline, key = "clear") {
        SpaceManager.clearHistoryAndCache()
        Toast.makeText(context, "인터넷 사용 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
    })
    add(ChromeMenuItemSpec("사이트 제어", Icons.Default.Tune, key = "site", dividerAfter = true) {
        if (isWebPage) onShowAdBlockDialog() else onShowChromeSettings()
    })
    add(ChromeMenuItemSpec("다운로드", Icons.Default.DownloadDone, key = "download") {
        SpaceManager.openDownloadsTab(activeSpace.id)
    })
    add(ChromeMenuItemSpec("북마크", Icons.Default.StarBorder, key = "bookmark") { onShowBookmarkManager() })
    add(ChromeMenuItemSpec("북마크 바", Icons.Default.BookmarkBorder, key = "bmbar", checked = prefs.showBookmarkBar, isToggleCheckbox = true) {
        SpaceManager.updatePreferences { it.copy(showBookmarkBar = !it.showBookmarkBar) }
    })
    add(ChromeMenuItemSpec("최근 탭", Icons.Default.Devices, key = "recent", dividerAfter = true) {
        if (!recentlyClosedEmpty) {
            SpaceManager.reopenLastClosedTab(activeSpace.id)
        } else {
            Toast.makeText(context, "최근 닫은 탭이 없습니다", Toast.LENGTH_SHORT).show()
        }
    })
    add(
        ChromeMenuItemSpec(
            title = "확대/축소",
            icon = Icons.Default.ZoomIn,
            key = "zoom",
            dividerAfter = true,
            zoomPercent = prefs.textZoom,
            onZoomOut = {
                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom - 10).coerceIn(75, 150)) }
            },
            onZoomReset = {
                SpaceManager.updatePreferences { it.copy(textZoom = 100) }
            },
            onZoomIn = {
                SpaceManager.updatePreferences { it.copy(textZoom = (it.textZoom + 10).coerceIn(75, 150)) }
            }
        )
    )
    if (isWebPage) {
        add(ChromeMenuItemSpec("공유", Icons.Default.Share, key = "share") {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                putExtra(Intent.EXTRA_TEXT, activeTab.url)
            }
            context.startActivity(Intent.createChooser(shareIntent, "링크 공유"))
        })
        add(ChromeMenuItemSpec("링크 복사", Icons.Default.ContentCopy, key = "copy_link") {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("URL", activeTab.url))
            Toast.makeText(context, "링크 복사됨", Toast.LENGTH_SHORT).show()
        })
        add(ChromeMenuItemSpec("PDF 저장", Icons.Default.Download, key = "pdf") {
            SpaceManager.printActiveTab(context, activeSpace.id)
        })
        add(ChromeMenuItemSpec("페이지 찾기", Icons.Default.FindInPage, key = "find") { onShowFindBar() })
        add(ChromeMenuItemSpec("번역", Icons.Default.Translate, key = "translate") {
            onTriggerTranslation("auto", prefs.defaultTranslateTarget)
        })
        add(ChromeMenuItemSpec("읽기 모드", Icons.Default.MenuBook, key = "reader") {
            scope.launch {
                runCatching {
                    val raw = SpaceManager.evaluateJsInExactSpace(activeSpace.id, BrowserScripts.MARKDOWN_EXTRACT_JS)
                    val md = JSONObject(raw).optString("markdown", "")
                    onSetReaderMarkdown(md.ifBlank { "본문을 추출할 수 없는 페이지입니다." })
                }
            }
        })
        add(ChromeMenuItemSpec("홈 바로가기", Icons.Default.AddToHomeScreen, key = "shortcut", dividerAfter = true) {
            runCatching {
                val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeTab.url)).apply {
                    setPackage(context.packageName)
                }
                val shortcut = ShortcutInfoCompat.Builder(context, "ego_sc_${activeTab.url.hashCode()}")
                    .setShortLabel(activeTab.title.ifBlank { currentHost.ifBlank { "웹 페이지" } }.take(24))
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(launchIntent)
                    .build()
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                    Toast.makeText(context, "홈 화면 바로가기 추가 요청됨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "런처가 바로가기 고정을 지원하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
        })
        add(ChromeMenuItemSpec("데스크톱", Icons.Default.DesktopWindows, key = "desktop", checked = prefs.desktopModeEnabled, isToggleCheckbox = true) {
            SpaceManager.updatePreferences { it.copy(desktopModeEnabled = !it.desktopModeEnabled) }
            SpaceManager.reload(activeSpace.id)
        })
    }
    add(ChromeMenuItemSpec("어두운 테마", Icons.Default.Contrast, key = "dark", checked = isEffectiveDark, isToggleCheckbox = true, dividerAfter = true) {
        val nextDark = !isEffectiveDark
        SpaceManager.updatePreferences {
            it.copy(darkModeEnabled = nextDark, themeMode = if (nextDark) ThemeMode.DARK else ThemeMode.LIGHT)
        }
    })
    add(ChromeMenuItemSpec("광고 차단", Icons.Default.Shield, key = "adblock", checked = prefs.adBlockEnabled, isToggleCheckbox = true) { onShowAdBlockDialog() })
    val activeWvForExt = SpaceManager.getActiveWebView(activeSpace.id)
    ExtensionManager.extensions.value.forEach { ext ->
        val isExtRunning = ExtensionManager.isRunningOnTab(ext, activeWvForExt, activeTab.url, activeExtPopupId)
        add(
            ChromeMenuItemSpec(
                title = ext.displayName,
                icon = Icons.Default.Extension,
                key = "ext:${ext.id}",
                checked = isExtRunning,
                bold = isExtRunning,
                onLongClick = onLongPressExtension?.let { cb -> { cb(ext) } }
            ) {
                if (!ext.enabled) {
                    onLongPressExtension?.invoke(ext)
                } else if (onTapExtension != null) {
                    onTapExtension(ext)
                } else {
                    val wv = SpaceManager.getActiveWebView(activeSpace.id)
                    if (wv != null) {
                        ExtensionManager.injectMatchingExtensions(wv, activeTab.url, "end", forceExtId = ext.id)
                        Toast.makeText(context, "${ext.displayName} 실행됨", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "실행할 웹페이지가 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    val gAcc = GoogleAccountManager.state.value
    val profileTitle = if (gAcc.isSignedIn && gAcc.displayName.isNotBlank()) gAcc.displayName else "Google 계정"
    add(
        ChromeMenuItemSpec(
            title = profileTitle,
            subtitle = if (gAcc.isSignedIn) "Google 계정" else null,
            icon = Icons.Default.Person,
            key = "profile",
            checked = gAcc.isSignedIn,
            bold = gAcc.isSignedIn
        ) { onShowChromeSettings() }
    )
    add(ChromeMenuItemSpec("설정", Icons.Default.Settings, key = "settings") { onShowChromeSettings() })
    add(ChromeMenuItemSpec("기본 브라우저", Icons.Default.Language, key = "default_app") {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }.onFailure {
            onShowChromeSettings()
        }
    })
}

@Composable
fun BrowserFullScreenFeatureOverlays(
    context: Context,
    spaces: List<BrowserSpace>,
    activeSpace: BrowserSpace,
    activeTab: BrowserTab,
    isEffectiveDark: Boolean,
    canAddAiSpace: Boolean,
    managingExt: InstalledExtension?,
    activeExtPopup: InstalledExtension?,
    duplicateDownloadPrompt: DuplicateDownloadPrompt?,
    showDownloadsScreen: Boolean,
    showChromeSettings: Boolean,
    settingsInitialCategory: SettingsCategory,
    showAllTabsDialog: Boolean,
    onUpdateManagingExt: (InstalledExtension?) -> Unit,
    onDismissExtPopup: () -> Unit,
    onDismissDownloadsScreen: () -> Unit,
    onOpenMcpDrawer: () -> Unit,
    onDismissChromeSettings: () -> Unit,
    onOpenChromeSettings: () -> Unit,
    onDismissAllTabsDialog: () -> Unit
) {
    managingExt?.let { ext ->
        ExtensionQuickManageDialog(
            ext = ext,
            onRunNow = {
                val wv = SpaceManager.getActiveWebView(activeSpace.id)
                if (wv != null) {
                    ExtensionManager.injectMatchingExtensions(wv, activeTab.url, "end", forceExtId = ext.id)
                    Toast.makeText(context, "${ext.name} 강제 실행됨", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleEnabled = { on ->
                ExtensionManager.toggleEnabled(ext.id, on)
                onUpdateManagingExt(ext.copy(enabled = on))
            },
            onHideFromToolbar = {
                ExtensionManager.toggleShowInToolbar(ext.id, false)
                Toast.makeText(context, "${ext.name} 툴바에서 숨김", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                ExtensionManager.deleteExtension(ext.id)
                Toast.makeText(context, "${ext.name} 삭제됨", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { onUpdateManagingExt(null) }
        )
    }
    activeExtPopup?.let { ext ->
        ExtensionPopupWebViewDialog(
            ext = ext,
            onDismiss = onDismissExtPopup
        )
    }
    duplicateDownloadPrompt?.let { prompt ->
        ChromeDuplicateDownloadDialog(
            prompt = prompt,
            onDismiss = { DownloadCenter.dismissDuplicatePrompt() }
        )
    }
    AnimatedVisibility(
        visible = showDownloadsScreen,
        modifier = Modifier.zIndex(50f),
        enter = slideInHorizontally(EgoMotion.intOffsetSpring) { it / 3 } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(190)) { it / 3 } + fadeOut(tween(160))
    ) {
        ChromeDownloadsScreen(onDismiss = onDismissDownloadsScreen)
    }
    AnimatedVisibility(
        visible = showChromeSettings,
        modifier = Modifier.zIndex(50f),
        enter = slideInHorizontally(EgoMotion.intOffsetSpring) { it / 3 } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(190)) { it / 3 } + fadeOut(tween(160))
    ) {
        ChromeSettingsScreen(
            initialCategory = settingsInitialCategory,
            onOpenMcpDrawer = onOpenMcpDrawer,
            onOpenGoogleSignInTab = {
                SpaceManager.createTab(activeSpace.id, initialUrl = "https://myaccount.google.com/", selectImmediately = true)
            },
            onExtractGoogleProfile = { cb ->
                val wv = SpaceManager.getActiveWebView(activeSpace.id)
                if (wv != null) {
                    GoogleAccountManager.extractProfileFromWebView(wv, cb)
                } else {
                    cb(false, "Google 페이지가 열린 탭이 없습니다.")
                }
            },
            onDismiss = onDismissChromeSettings
        )
    }
    AnimatedVisibility(
        visible = showAllTabsDialog,
        modifier = Modifier.zIndex(50f),
        enter = scaleIn(animationSpec = EgoMotion.smoothSpring(), initialScale = 0.90f) + fadeIn(tween(180)),
        exit = scaleOut(animationSpec = tween(150), targetScale = 0.92f) + fadeOut(tween(140))
    ) {
        ChromeTabSwitcherOverlay(
            spaces = spaces,
            activeSpace = activeSpace,
            darkTheme = isEffectiveDark,
            canAddAiSpace = canAddAiSpace,
            onSelectSpaceAndTab = { targetSpaceId, tabId ->
                SpaceManager.selectSpace(targetSpaceId)
                SpaceManager.selectTab(targetSpaceId, tabId)
                onDismissAllTabsDialog()
            },
            onCloseTab = { targetSpaceId, tabId -> SpaceManager.closeTab(targetSpaceId, tabId) },
            onMoveTab = { targetSpaceId, fromId, toId -> SpaceManager.moveTabById(targetSpaceId, fromId, toId) },
            onCreateTab = { targetSpaceId, incog ->
                SpaceManager.selectSpace(targetSpaceId)
                SpaceManager.createTab(targetSpaceId, incognito = incog)
                onDismissAllTabsDialog()
            },
            onCreateAiSpace = { SpaceManager.createAiSpace()?.let { SpaceManager.selectSpace(it.id) } },
            onCloseAiSpace = { targetSpaceId -> SpaceManager.closeAiSpace(targetSpaceId) },
            onCloseAllTabs = { targetSpaceId ->
                SpaceManager.closeAllTabsInSpace(targetSpaceId)
                onDismissAllTabsDialog()
            },
            onClearBrowsingData = {
                SpaceManager.clearHistoryAndCache()
                Toast.makeText(context, "인터넷 사용 기록 및 캐시 삭제됨", Toast.LENGTH_SHORT).show()
            },
            onOpenSettings = {
                onDismissAllTabsDialog()
                onOpenChromeSettings()
            },
            onDismiss = onDismissAllTabsDialog
        )
    }
}
