package com.egolite.mobile.browser

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

enum class ExtensionCompatStatus(val label: String) {
    FULL("완전 호환"),
    PARTIAL("부분 호환"),
    UNSUPPORTED("이식 불가")
}

data class ExtensionContentScript(
    val matches: List<String>,
    val jsFiles: List<String>,
    val cssFiles: List<String>,
    val runAt: String = "document_idle",
    val world: String = "ISOLATED"
)

data class ExtensionCompatReport(
    val status: ExtensionCompatStatus,
    val name: String,
    val version: String,
    val description: String,
    val manifestVersion: Int,
    val supportedFeatures: List<String>,
    val unsupportedFeatures: List<String>,
    val warnings: List<String>,
    val canInstall: Boolean,
    val tempDir: File,
    val iconRelPath: String?,
    val popupRelPath: String?,
    val backgroundScript: String?,
    val contentScripts: List<ExtensionContentScript>,
    val hostPermissions: List<String>
)

data class InstalledExtension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val enabled: Boolean = true,
    val showInToolbar: Boolean = true,
    val compatStatus: ExtensionCompatStatus = ExtensionCompatStatus.FULL,
    val iconPath: String? = null,
    val popupPath: String? = null,
    val backgroundScript: String? = null,
    val contentScripts: List<ExtensionContentScript> = emptyList(),
    val dirPath: String,
    val installedAtMs: Long = System.currentTimeMillis()
)

object ExtensionManager {
    private const val PREF_META = "ego_extensions_meta_v1"
    private const val PREF_STORAGE_PREFIX = "ego_ext_store_"
    private const val KEY_LIST = "installed_json"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var appContext: Context? = null
    private var metaPrefs: SharedPreferences? = null

    private val _extensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val extensions: StateFlow<List<InstalledExtension>> = _extensions.asStateFlow()

    private val _pendingReport = MutableStateFlow<ExtensionCompatReport?>(null)
    val pendingReport: StateFlow<ExtensionCompatReport?> = _pendingReport.asStateFlow()

    private val bgWebViews = ConcurrentHashMap<String, WebView>()
    private val pendingCallbacks = ConcurrentHashMap<String, (String) -> Unit>()

    private val UNSUPPORTED_PERMISSIONS = setOf(
        "declarativeNetRequest",
        "declarativeNetRequestWithHostAccess",
        "webRequest",
        "webRequestBlocking",
        "cookies",
        "tabCapture",
        "desktopCapture",
        "nativeMessaging",
        "debugger",
        "proxy",
        "vpnProvider"
    )

    private val UNSUPPORTED_APIS = listOf(
        "chrome.declarativeNetRequest" to "네트워크 규칙 엔진(declarativeNetRequest)",
        "chrome.webRequest" to "저수준 요청 가로채기(webRequest)",
        "chrome.cookies" to "브라우저 쿠키 직접 제어(cookies)",
        "chrome.tabCapture" to "탭 화면 캡처(tabCapture)",
        "chrome.desktopCapture" to "데스크톱 화면 캡처(desktopCapture)",
        "chrome.runtime.connectNative" to "PC 네이티브 메시징(nativeMessaging)",
        "chrome.debugger" to "크롬 디버거 프로토콜(debugger)"
    )

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        metaPrefs = appContext!!.getSharedPreferences(PREF_META, Context.MODE_PRIVATE)
        loadPersisted()
    }

    private fun loadPersisted() {
        val raw = metaPrefs?.getString(KEY_LIST, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<InstalledExtension>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val dir = o.optString("dirPath")
                if (dir.isBlank() || !File(dir).exists()) continue
                val csArr = o.optJSONArray("contentScripts") ?: JSONArray()
                val scripts = mutableListOf<ExtensionContentScript>()
                for (j in 0 until csArr.length()) {
                    val cso = csArr.getJSONObject(j)
                    scripts.add(
                        ExtensionContentScript(
                            matches = cso.optJSONArray("matches").toStringList(),
                            jsFiles = cso.optJSONArray("js").toStringList(),
                            cssFiles = cso.optJSONArray("css").toStringList(),
                            runAt = cso.optString("runAt", "document_idle"),
                            world = cso.optString("world", "ISOLATED")
                        )
                    )
                }
                val status = runCatching {
                    ExtensionCompatStatus.valueOf(o.optString("compatStatus", "FULL"))
                }.getOrDefault(ExtensionCompatStatus.FULL)
                list.add(
                    InstalledExtension(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        version = o.optString("version", "1.0"),
                        description = o.optString("description", ""),
                        enabled = o.optBoolean("enabled", true),
                        showInToolbar = o.optBoolean("showInToolbar", true),
                        compatStatus = status,
                        iconPath = o.optString("iconPath").takeIf { it.isNotBlank() },
                        popupPath = o.optString("popupPath").takeIf { it.isNotBlank() },
                        backgroundScript = o.optString("backgroundScript").takeIf { it.isNotBlank() },
                        contentScripts = scripts,
                        dirPath = dir,
                        installedAtMs = o.optLong("installedAtMs", System.currentTimeMillis())
                    )
                )
            }
            _extensions.value = list
        }
    }

    private fun persist(list: List<InstalledExtension>) {
        _extensions.value = list
        val arr = JSONArray()
        for (ext in list) {
            val csArr = JSONArray()
            for (cs in ext.contentScripts) {
                csArr.put(
                    JSONObject()
                        .put("matches", JSONArray(cs.matches))
                        .put("js", JSONArray(cs.jsFiles))
                        .put("css", JSONArray(cs.cssFiles))
                        .put("runAt", cs.runAt)
                        .put("world", cs.world)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", ext.id)
                    .put("name", ext.name)
                    .put("version", ext.version)
                    .put("description", ext.description)
                    .put("enabled", ext.enabled)
                    .put("showInToolbar", ext.showInToolbar)
                    .put("compatStatus", ext.compatStatus.name)
                    .put("iconPath", ext.iconPath ?: "")
                    .put("popupPath", ext.popupPath ?: "")
                    .put("backgroundScript", ext.backgroundScript ?: "")
                    .put("contentScripts", csArr)
                    .put("dirPath", ext.dirPath)
                    .put("installedAtMs", ext.installedAtMs)
            )
        }
        metaPrefs?.edit()?.putString(KEY_LIST, arr.toString())?.apply()
    }

    fun inspectZipUri(context: Context, uri: Uri, onReady: (ExtensionCompatReport) -> Unit) {
        init(context)
        ioScope.launch {
            val report = runCatching { inspectZipBlocking(context, uri) }.getOrElse { err ->
                val dummyDir = File(context.cacheDir, "ext_err_${System.nanoTime()}").apply { mkdirs() }
                ExtensionCompatReport(
                    status = ExtensionCompatStatus.UNSUPPORTED,
                    name = "알 수 없는 파일",
                    version = "-",
                    description = err.message ?: "ZIP 압축 해제 실패",
                    manifestVersion = 0,
                    supportedFeatures = emptyList(),
                    unsupportedFeatures = listOf("유효한 크롬 확장 ZIP 형식이 아닙니다: ${err.message}"),
                    warnings = emptyList(),
                    canInstall = false,
                    tempDir = dummyDir,
                    iconRelPath = null,
                    popupRelPath = null,
                    backgroundScript = null,
                    contentScripts = emptyList(),
                    hostPermissions = emptyList()
                )
            }
            _pendingReport.value = report
            mainHandler.post { onReady(report) }
        }
    }

    fun dismissPendingReport(cleanupTemp: Boolean = true) {
        val cur = _pendingReport.value
        _pendingReport.value = null
        if (cleanupTemp && cur != null && cur.tempDir.path.contains("ext_stage_")) {
            ioScope.launch { runCatching { cur.tempDir.deleteRecursively() } }
        }
    }

    private fun inspectZipBlocking(context: Context, uri: Uri): ExtensionCompatReport {
        val stageDir = File(context.cacheDir, "ext_stage_${System.currentTimeMillis()}").apply { mkdirs() }
        val canonicalStage = stageDir.canonicalPath
        context.contentResolver.openInputStream(uri)?.use { rawIn ->
            ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(stageDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(canonicalStage + File.separator)) {
                        throw SecurityException("잘못된 ZIP 경로가 포함되어 있습니다.")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("선택한 파일을 열 수 없습니다.")

        // Locate manifest.json (root or single nested directory)
        val manifestFile = findManifestFile(stageDir)
            ?: return ExtensionCompatReport(
                status = ExtensionCompatStatus.UNSUPPORTED,
                name = "manifest.json 없음",
                version = "-",
                description = "크롬 확장 프로그램의 핵심 설정 파일(manifest.json)을 찾을 수 없습니다.",
                manifestVersion = 0,
                supportedFeatures = emptyList(),
                unsupportedFeatures = listOf("manifest.json 누락 (크롬 확장 ZIP이 아님)"),
                warnings = emptyList(),
                canInstall = false,
                tempDir = stageDir,
                iconRelPath = null,
                popupRelPath = null,
                backgroundScript = null,
                contentScripts = emptyList(),
                hostPermissions = emptyList()
            )

        val rootDir = manifestFile.parentFile ?: stageDir
        val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val mv = manifest.optInt("manifest_version", 3)
        val name = manifest.optString("name", "이름 없는 확장").ifBlank { "이름 없는 확장" }
        val version = manifest.optString("version", "1.0")
        val description = manifest.optString("description", "")

        val permissions = manifest.optJSONArray("permissions").toStringList()
        val hostPermissions = manifest.optJSONArray("host_permissions").toStringList()

        // Parse content_scripts
        val csList = mutableListOf<ExtensionContentScript>()
        val csArr = manifest.optJSONArray("content_scripts")
        if (csArr != null) {
            for (i in 0 until csArr.length()) {
                val o = csArr.getJSONObject(i)
                csList.add(
                    ExtensionContentScript(
                        matches = o.optJSONArray("matches").toStringList(),
                        jsFiles = o.optJSONArray("js").toStringList(),
                        cssFiles = o.optJSONArray("css").toStringList(),
                        runAt = o.optString("run_at", "document_idle"),
                        world = o.optString("world", "ISOLATED")
                    )
                )
            }
        }

        // Parse popup & icon & background
        val actionObj = manifest.optJSONObject("action") ?: manifest.optJSONObject("browser_action")
        val popupRel = actionObj?.optString("default_popup")?.takeIf { it.isNotBlank() && File(rootDir, it).exists() }
        val iconsObj = manifest.optJSONObject("icons")
        val iconRel = listOf("128", "48", "32", "16")
            .mapNotNull { iconsObj?.optString(it)?.takeIf { p -> p.isNotBlank() && File(rootDir, p).exists() } }
            .firstOrNull()

        val bgObj = manifest.optJSONObject("background")
        val bgScript = bgObj?.optString("service_worker")?.takeIf { it.isNotBlank() && File(rootDir, it).exists() }
            ?: bgObj?.optJSONArray("scripts")?.optString(0)?.takeIf { it.isNotBlank() && File(rootDir, it).exists() }

        val supported = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (csList.isNotEmpty()) {
            val totalJs = csList.sumOf { it.jsFiles.size }
            val totalCss = csList.sumOf { it.cssFiles.size }
            val hasMain = csList.any { it.world.equals("MAIN", ignoreCase = true) }
            supported.add("웹페이지 스크립트/스타일 자동 주입 (JS ${totalJs}개 · CSS ${totalCss}개${if (hasMain) " · MAIN 월드 포함" else ""})")
        }
        if (popupRel != null) {
            supported.add("툴바 버튼 팝업 UI ($popupRel)")
        }
        if ("storage" in permissions) {
            supported.add("로컬 설정·즐겨찾기 저장 (chrome.storage.local 폴리필)")
        }
        if (bgScript != null) {
            supported.add("백그라운드 워커 및 CORS 우회 통신 ($bgScript)")
            warnings.add("백그라운드 스크립트는 경량 WebView + 네이티브 Fetch 프록시로 동작합니다.")
        }
        if (hostPermissions.isNotEmpty()) {
            supported.add("외부 도메인 CORS 우회 요청 (${hostPermissions.size}개 호스트)")
        }

        for (perm in permissions) {
            if (perm in UNSUPPORTED_PERMISSIONS) {
                unsupported.add("미지원 권한: $perm")
            }
        }
        if (manifest.has("devtools_page")) {
            unsupported.add("개발자 도구 패널(devtools_page) 미지원")
        }

        // Scan JS files for unsupported chrome.* calls
        val allJsTexts = rootDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("js", ignoreCase = true) && it.length() < 1_500_000L }
            .map { runCatching { it.readText(Charsets.UTF_8) }.getOrDefault("") }
            .toList()
        for ((api, label) in UNSUPPORTED_APIS) {
            if (allJsTexts.any { it.contains(api) }) {
                unsupported.add("미지원 API 사용: $label")
            }
        }

        val hasRunnable = csList.isNotEmpty() || popupRel != null || bgScript != null
        val status = when {
            !hasRunnable -> ExtensionCompatStatus.UNSUPPORTED
            unsupported.isEmpty() -> ExtensionCompatStatus.FULL
            csList.isNotEmpty() || popupRel != null -> ExtensionCompatStatus.PARTIAL
            else -> ExtensionCompatStatus.UNSUPPORTED
        }
        if (!hasRunnable) {
            unsupported.add("실행 가능한 콘텐츠 스크립트나 팝업 UI가 없습니다.")
        }

        return ExtensionCompatReport(
            status = status,
            name = name,
            version = version,
            description = description,
            manifestVersion = mv,
            supportedFeatures = supported.distinct(),
            unsupportedFeatures = unsupported.distinct(),
            warnings = warnings.distinct(),
            canInstall = status != ExtensionCompatStatus.UNSUPPORTED,
            tempDir = rootDir,
            iconRelPath = iconRel,
            popupRelPath = popupRel,
            backgroundScript = bgScript,
            contentScripts = csList,
            hostPermissions = hostPermissions
        )
    }

    private fun findManifestFile(dir: File): File? {
        val direct = File(dir, "manifest.json")
        if (direct.isFile) return direct
        val subDirs = dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith("__MACOSX") } ?: emptyList()
        for (sub in subDirs) {
            val f = File(sub, "manifest.json")
            if (f.isFile) return f
        }
        return null
    }

    fun confirmInstall(context: Context, report: ExtensionCompatReport): InstalledExtension? {
        init(context)
        if (!report.canInstall) return null
        val id = "ext_" + UUID.randomUUID().toString().replace("-", "").take(10)
        val extRoot = File(context.filesDir, "extensions/$id").apply { mkdirs() }
        runCatching {
            report.tempDir.copyRecursively(extRoot, overwrite = true)
        }.onFailure { return null }

        val installed = InstalledExtension(
            id = id,
            name = report.name,
            version = report.version,
            description = report.description,
            enabled = true,
            showInToolbar = true,
            compatStatus = report.status,
            iconPath = report.iconRelPath?.let { File(extRoot, it).absolutePath },
            popupPath = report.popupRelPath?.let { File(extRoot, it).absolutePath },
            backgroundScript = report.backgroundScript,
            contentScripts = report.contentScripts,
            dirPath = extRoot.absolutePath,
            installedAtMs = System.currentTimeMillis()
        )
        persist(_extensions.value + installed)
        dismissPendingReport(cleanupTemp = true)
        if (installed.backgroundScript != null) {
            mainHandler.post { ensureBackgroundRunner(installed) }
        }
        return installed
    }

    fun toggleEnabled(extId: String, enabled: Boolean) {
        val updated = _extensions.value.map { if (it.id == extId) it.copy(enabled = enabled) else it }
        persist(updated)
        if (!enabled) {
            mainHandler.post { destroyBackgroundRunner(extId) }
        } else {
            updated.firstOrNull { it.id == extId }?.let { ext ->
                if (ext.backgroundScript != null) mainHandler.post { ensureBackgroundRunner(ext) }
            }
        }
    }

    fun toggleShowInToolbar(extId: String, show: Boolean) {
        persist(_extensions.value.map { if (it.id == extId) it.copy(showInToolbar = show) else it })
    }

    fun deleteExtension(extId: String) {
        val target = _extensions.value.firstOrNull { it.id == extId } ?: return
        mainHandler.post { destroyBackgroundRunner(extId) }
        persist(_extensions.value.filterNot { it.id == extId })
        ioScope.launch {
            runCatching { File(target.dirPath).deleteRecursively() }
            runCatching { appContext?.getSharedPreferences(PREF_STORAGE_PREFIX + extId, Context.MODE_PRIVATE)?.edit()?.clear()?.apply() }
        }
    }

    // -------------------------------------------------------------------------
    // Chrome Match Pattern & Content Script Injection
    // -------------------------------------------------------------------------
    fun matchesUrl(pattern: String, url: String): Boolean {
        if (pattern == "<all_urls>" || pattern == "*://*/*") return url.startsWith("http://") || url.startsWith("https://")
        val regexStr = buildString {
            append("^")
            for (ch in pattern) {
                when (ch) {
                    '*' -> append(".*")
                    '.', '?', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|', '\\' -> append("\\").append(ch)
                    else -> append(ch)
                }
            }
            append("$")
        }
        return runCatching { Regex(regexStr, RegexOption.IGNORE_CASE).matches(url) }.getOrDefault(false)
    }

    fun injectMatchingExtensions(webView: WebView, url: String?, phase: String, forceExtId: String? = null) {
        if (url.isNullOrBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) return
        val list = _extensions.value.filter { it.enabled && (forceExtId == null || it.id == forceExtId) }
        if (list.isEmpty()) return

        for (ext in list) {
            val matching = ext.contentScripts.filter { cs ->
                val runMatch = forceExtId != null || when (phase) {
                    "start" -> cs.runAt == "document_start"
                    "end" -> cs.runAt == "document_end" || cs.runAt == "document_idle" || cs.runAt.isBlank()
                    else -> false
                }
                runMatch && cs.matches.any { pat -> matchesUrl(pat, url) }
            }
            if (matching.isEmpty()) continue

            if (ext.backgroundScript != null) {
                ensureBackgroundRunner(ext)
            }

            // 1. Inject lightweight Chrome API Polyfill scoped for this extension
            webView.evaluateJavascript(buildPagePolyfillJs(ext), null)

            // 2. Inject CSS & JS files in manifest order
            val root = File(ext.dirPath)
            for (cs in matching) {
                for (cssRel in cs.cssFiles) {
                    val cssFile = File(root, cssRel)
                    if (cssFile.isFile) {
                        val b64 = Base64.encodeToString(cssFile.readBytes(), Base64.NO_WRAP)
                        val cssJs = """
                            (function(){
                              var id='ego-ext-css-${ext.id}-${cssRel.hashCode()}';
                              if(document.getElementById(id)) return;
                              var st=document.createElement('style');
                              st.id=id;
                              st.textContent=decodeURIComponent(escape(atob('$b64')));
                              (document.head||document.documentElement).appendChild(st);
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(cssJs, null)
                    }
                }
                for (jsRel in cs.jsFiles) {
                    val jsFile = File(root, jsRel)
                    if (jsFile.isFile) {
                        val code = runCatching { jsFile.readText(Charsets.UTF_8) }.getOrNull() ?: continue
                        val wrapped = """
                            (function(){
                              try {
                                var chrome = window.__egoExtGetChrome ? window.__egoExtGetChrome('${ext.id}') : window.chrome;
                                $code
                              } catch (e) { console.warn('[EgoExt:${ext.id}]', e); }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(wrapped, null)
                    }
                }
            }
        }
    }

    private fun buildPagePolyfillJs(ext: InstalledExtension): String {
        val manifestJson = JSONObject()
            .put("name", ext.name)
            .put("version", ext.version)
            .put("manifest_version", 3)
            .toString()
        return """
            (function(){
              if (!window.__egoExtCallbacks) window.__egoExtCallbacks = {};
              if (!window.__egoExtStorageListeners) window.__egoExtStorageListeners = {};
              window.__egoExtResolveMsg = function(reqId, respJson) {
                var cb = window.__egoExtCallbacks[reqId];
                if (cb) {
                  delete window.__egoExtCallbacks[reqId];
                  try { cb(respJson ? JSON.parse(respJson) : undefined); } catch(e) { cb(undefined); }
                }
              };
              if (!window.__egoExtGetChrome) {
                window.__egoExtGetChrome = function(extId) {
                  if (!window.__egoExtInstances) window.__egoExtInstances = {};
                  if (window.__egoExtInstances[extId]) return window.__egoExtInstances[extId];
                  var c = {
                    runtime: {
                      id: extId,
                      lastError: null,
                      getManifest: function() { return $manifestJson; },
                      sendMessage: function(msg, cb) {
                        return new Promise(function(resolve) {
                          var reqId = extId + '_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
                          window.__egoExtCallbacks[reqId] = function(res) {
                            if (typeof cb === 'function') { try { cb(res); } catch(e){} }
                            resolve(res);
                          };
                          if (window.EgoExtBridge && window.EgoExtBridge.sendMessage) {
                            window.EgoExtBridge.sendMessage(extId, reqId, JSON.stringify(msg || {}));
                          } else {
                            window.__egoExtResolveMsg(reqId, 'null');
                          }
                        });
                      }
                    },
                    storage: {
                      local: {
                        get: function(keys, cb) {
                          return new Promise(function(resolve) {
                            var raw = (window.EgoExtBridge && window.EgoExtBridge.storageGet)
                              ? window.EgoExtBridge.storageGet(extId, typeof keys === 'string' ? JSON.stringify([keys]) : JSON.stringify(keys || null))
                              : '{}';
                            var obj = {};
                            try { obj = JSON.parse(raw || '{}'); } catch(e) {}
                            if (typeof cb === 'function') { try { cb(obj); } catch(e){} }
                            resolve(obj);
                          });
                        },
                        set: function(items, cb) {
                          return new Promise(function(resolve) {
                            if (window.EgoExtBridge && window.EgoExtBridge.storageSet) {
                              window.EgoExtBridge.storageSet(extId, JSON.stringify(items || {}));
                            }
                            var listeners = window.__egoExtStorageListeners[extId] || [];
                            var changes = {};
                            for (var k in items) { changes[k] = { newValue: items[k] }; }
                            listeners.forEach(function(fn){ try { fn(changes, 'local'); } catch(e){} });
                            if (typeof cb === 'function') { try { cb(); } catch(e){} }
                            resolve();
                          });
                        },
                        remove: function(keys, cb) {
                          return new Promise(function(resolve) {
                            if (window.EgoExtBridge && window.EgoExtBridge.storageRemove) {
                              window.EgoExtBridge.storageRemove(extId, typeof keys === 'string' ? JSON.stringify([keys]) : JSON.stringify(keys || []));
                            }
                            if (typeof cb === 'function') { try { cb(); } catch(e){} }
                            resolve();
                          });
                        }
                      },
                      onChanged: {
                        addListener: function(fn) {
                          if (!window.__egoExtStorageListeners[extId]) window.__egoExtStorageListeners[extId] = [];
                          window.__egoExtStorageListeners[extId].push(fn);
                        },
                        removeListener: function(fn) {
                          var arr = window.__egoExtStorageListeners[extId];
                          if (!arr) return;
                          var idx = arr.indexOf(fn);
                          if (idx >= 0) arr.splice(idx, 1);
                        }
                      }
                    }
                  };
                  window.__egoExtInstances[extId] = c;
                  return c;
                };
              }
            })();
        """.trimIndent()
    }

    // -------------------------------------------------------------------------
    // Hidden Offscreen Background Service Worker Runner (CORS-free native fetch!)
    // -------------------------------------------------------------------------
    private fun ensureBackgroundRunner(ext: InstalledExtension) {
        val ctx = appContext ?: return
        val bgRel = ext.backgroundScript ?: return
        if (bgWebViews.containsKey(ext.id)) return
        val bgFile = File(ext.dirPath, bgRel)
        if (!bgFile.isFile) return
        val bgCode = runCatching { bgFile.readText(Charsets.UTF_8) }.getOrNull() ?: return

        val wv = WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(BackgroundBridge(ext.id), "EgoBgBridge")
        }
        bgWebViews[ext.id] = wv

        val manifestJson = JSONObject()
            .put("name", ext.name)
            .put("version", ext.version)
            .put("manifest_version", 3)
            .toString()

        val b64Code = Base64.encodeToString(bgCode.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val html = """
            <!doctype html><html><head><meta charset="utf-8"><script>
            window.__bgFetchPending = {};
            window.__bgMsgListeners = [];
            window.__bgResolveFetch = function(fid, status, ok, bodyB64) {
              var p = window.__bgFetchPending[fid];
              if (!p) return;
              delete window.__bgFetchPending[fid];
              if (status < 0) { p.reject(new Error('Network error')); return; }
              var txt = '';
              try { txt = decodeURIComponent(escape(atob(bodyB64 || ''))); } catch(e) { try { txt = atob(bodyB64 || ''); } catch(e2){} }
              p.resolve({
                ok: !!ok,
                status: status,
                text: function(){ return Promise.resolve(txt); },
                json: function(){ return Promise.resolve(JSON.parse(txt)); },
                headers: { get: function(){ return ''; } }
              });
            };
            window.fetch = function(url, init) {
              return new Promise(function(resolve, reject) {
                var fid = 'f_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
                window.__bgFetchPending[fid] = { resolve: resolve, reject: reject };
                var method = (init && init.method) ? init.method : 'GET';
                var body = (init && typeof init.body === 'string') ? init.body : '';
                EgoBgBridge.nativeFetch(fid, String(url), method, body);
              });
            };
            window.chrome = {
              runtime: {
                id: '${ext.id}',
                getManifest: function(){ return $manifestJson; },
                onInstalled: { addListener: function(){} },
                onMessage: { addListener: function(fn){ window.__bgMsgListeners.push(fn); } }
              },
              permissions: {
                contains: function(p, cb){ if(cb) cb(true); return Promise.resolve(true); }
              },
              tabs: { query: function(){ return Promise.resolve([]); } },
              scripting: {
                executeScript: function(){ return Promise.resolve([]); },
                insertCSS: function(){ return Promise.resolve(); }
              },
              storage: {
                local: {
                  get: function(keys, cb) {
                    return new Promise(function(resolve) {
                      var raw = EgoBgBridge.storageGet(typeof keys === 'string' ? JSON.stringify([keys]) : JSON.stringify(keys || null));
                      var obj = {};
                      try { obj = JSON.parse(raw || '{}'); } catch(e){}
                      if (typeof cb === 'function') cb(obj);
                      resolve(obj);
                    });
                  },
                  set: function(items, cb) {
                    return new Promise(function(resolve) {
                      EgoBgBridge.storageSet(JSON.stringify(items || {}));
                      if (typeof cb === 'function') cb();
                      resolve();
                    });
                  },
                  remove: function(keys, cb) {
                    return new Promise(function(resolve) {
                      EgoBgBridge.storageRemove(typeof keys === 'string' ? JSON.stringify([keys]) : JSON.stringify(keys || []));
                      if (typeof cb === 'function') cb();
                      resolve();
                    });
                  }
                }
              }
            };
            window.__bgDispatchMessage = function(reqId, msgJson) {
              var msg = {};
              try { msg = JSON.parse(msgJson); } catch(e){}
              var responded = false;
              function sendResp(val) {
                if (responded) return;
                responded = true;
                EgoBgBridge.replyMessage(reqId, JSON.stringify(val === undefined ? null : val));
              }
              var sender = { id: '${ext.id}' };
              var asyncHandled = false;
              for (var i = 0; i < window.__bgMsgListeners.length; i++) {
                try {
                  var ret = window.__bgMsgListeners[i](msg, sender, sendResp);
                  if (ret === true) asyncHandled = true;
                } catch(e) { console.error(e); }
              }
              if (!asyncHandled && !responded) sendResp(null);
            };
            try {
              var code = decodeURIComponent(escape(atob('$b64Code')));
              (new Function(code))();
            } catch(e) { console.error('[EgoBgInit]', e); }
            </script></head><body></body></html>
        """.trimIndent()
        wv.loadDataWithBaseURL("https://ego-extension.local/", html, "text/html", "UTF-8", null)
    }

    private fun destroyBackgroundRunner(extId: String) {
        bgWebViews.remove(extId)?.let { wv ->
            runCatching { wv.destroy() }
        }
    }

    private inner class BackgroundBridge(private val extId: String) {
        @JavascriptInterface
        fun nativeFetch(fid: String, urlStr: String, method: String, body: String) {
            ioScope.launch {
                val (status, ok, b64) = runCatching {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        requestMethod = method.ifBlank { "GET" }
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                        if (body.isNotEmpty() && (method == "POST" || method == "PUT")) {
                            doOutput = true
                            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                        }
                    }
                    val code = conn.responseCode
                    val stream = if (code in 200..399) conn.inputStream else conn.errorStream
                    val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
                    Triple(code, code in 200..299, Base64.encodeToString(bytes, Base64.NO_WRAP))
                }.getOrElse { Triple(-1, false, "") }

                mainHandler.post {
                    bgWebViews[extId]?.evaluateJavascript(
                        "window.__bgResolveFetch('$fid', $status, $ok, '$b64');",
                        null
                    )
                }
            }
        }

        @JavascriptInterface
        fun replyMessage(reqId: String, respJson: String) {
            val cb = pendingCallbacks.remove(reqId) ?: return
            mainHandler.post { cb(respJson) }
        }

        @JavascriptInterface
        fun storageGet(keysJson: String): String = storageGetInternal(extId, keysJson)

        @JavascriptInterface
        fun storageSet(itemsJson: String) = storageSetInternal(extId, itemsJson)

        @JavascriptInterface
        fun storageRemove(keysJson: String) = storageRemoveInternal(extId, keysJson)
    }

    /**
     * 페이지 WebView에 부착되는 통합 확장 브리지 (`EgoExtBridge`)
     */
    class PageExtensionBridge(private val pageWebView: WebView) {
        @JavascriptInterface
        fun sendMessage(extId: String, reqId: String, msgJson: String) {
            val ext = _extensions.value.firstOrNull { it.id == extId }
            if (ext == null || ext.backgroundScript == null) {
                mainHandler.post {
                    pageWebView.evaluateJavascript("window.__egoExtResolveMsg('$reqId', 'null');", null)
                }
                return
            }
            pendingCallbacks[reqId] = { respJson ->
                val escaped = JSONObject.quote(respJson)
                pageWebView.evaluateJavascript("window.__egoExtResolveMsg('$reqId', $escaped);", null)
            }
            mainHandler.post {
                ensureBackgroundRunner(ext)
                val escapedMsg = JSONObject.quote(msgJson)
                bgWebViews[extId]?.evaluateJavascript("window.__bgDispatchMessage('$reqId', $escapedMsg);", null)
            }
        }

        @JavascriptInterface
        fun storageGet(extId: String, keysJson: String): String = storageGetInternal(extId, keysJson)

        @JavascriptInterface
        fun storageSet(extId: String, itemsJson: String) = storageSetInternal(extId, itemsJson)

        @JavascriptInterface
        fun storageRemove(extId: String, keysJson: String) = storageRemoveInternal(extId, keysJson)
    }

    private fun storageGetInternal(extId: String, keysJson: String): String {
        val sp = appContext?.getSharedPreferences(PREF_STORAGE_PREFIX + extId, Context.MODE_PRIVATE) ?: return "{}"
        val result = JSONObject()
        val all = sp.all
        runCatching {
            val parsed = when {
                keysJson == "null" || keysJson.isBlank() -> null
                keysJson.trim().startsWith("[") -> JSONArray(keysJson).toStringList()
                keysJson.trim().startsWith("{") -> {
                    val defaults = JSONObject(keysJson)
                    val it = defaults.keys()
                    while (it.hasNext()) {
                        val k = it.next()
                        val stored = sp.getString(k, null)
                        if (stored != null) result.put(k, parseStoredJsonValue(stored)) else result.put(k, defaults.get(k))
                    }
                    return result.toString()
                }
                else -> listOf(keysJson)
            }
            val targetKeys = parsed ?: all.keys.toList()
            for (k in targetKeys) {
                val stored = sp.getString(k, null)
                if (stored != null) result.put(k, parseStoredJsonValue(stored))
            }
        }
        return result.toString()
    }

    private fun storageSetInternal(extId: String, itemsJson: String) {
        val sp = appContext?.getSharedPreferences(PREF_STORAGE_PREFIX + extId, Context.MODE_PRIVATE) ?: return
        runCatching {
            val obj = JSONObject(itemsJson)
            val ed = sp.edit()
            val it = obj.keys()
            while (it.hasNext()) {
                val k = it.next()
                val wrapped = JSONObject().put("v", obj.get(k)).toString()
                ed.putString(k, wrapped)
            }
            ed.apply()
        }
    }

    private fun storageRemoveInternal(extId: String, keysJson: String) {
        val sp = appContext?.getSharedPreferences(PREF_STORAGE_PREFIX + extId, Context.MODE_PRIVATE) ?: return
        runCatching {
            val keys = JSONArray(keysJson).toStringList()
            val ed = sp.edit()
            for (k in keys) ed.remove(k)
            ed.apply()
        }
    }

    private fun parseStoredJsonValue(raw: String): Any? {
        return runCatching { JSONObject(raw).opt("v") }.getOrDefault(raw)
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            val s = optString(i)
            if (s.isNotBlank()) out.add(s)
        }
        return out
    }
}
