package com.egolite.mobile.account

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.WebView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GoogleAccountState(
    val isSignedIn: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val oauthWebClientId: String = "",
    val authMode: String = "",
    val syncHistoryAndTabs: Boolean = true,
    val syncBookmarks: Boolean = true,
    val syncSettings: Boolean = true,
    val syncAiSpaces: Boolean = true,
    val lastSyncTimeMs: Long = 0L
)

object GoogleAccountManager {
    private const val PREF_NAME = "ego_google_account"
    private const val KEY_SIGNED_IN = "signed_in"
    private const val KEY_NAME = "display_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHOTO_URL = "photo_url"
    private const val KEY_WEB_CLIENT_ID = "oauth_web_client_id"
    private const val KEY_AUTH_MODE = "auth_mode"
    private const val KEY_SYNC_HISTORY = "sync_history"
    private const val KEY_SYNC_BOOKMARKS = "sync_bookmarks"
    private const val KEY_SYNC_SETTINGS = "sync_settings"
    private const val KEY_SYNC_AI = "sync_ai"
    private const val KEY_LAST_SYNC = "last_sync_ms"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(GoogleAccountState())
    val state: StateFlow<GoogleAccountState> = _state.asStateFlow()

    private val _avatarBitmap = MutableStateFlow<Bitmap?>(null)
    val avatarBitmap: StateFlow<Bitmap?> = _avatarBitmap.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val p = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = p
        val loaded = GoogleAccountState(
            isSignedIn = p.getBoolean(KEY_SIGNED_IN, false),
            displayName = p.getString(KEY_NAME, "") ?: "",
            email = p.getString(KEY_EMAIL, "") ?: "",
            photoUrl = p.getString(KEY_PHOTO_URL, "") ?: "",
            oauthWebClientId = p.getString(KEY_WEB_CLIENT_ID, "") ?: "",
            authMode = p.getString(KEY_AUTH_MODE, "") ?: "",
            syncHistoryAndTabs = p.getBoolean(KEY_SYNC_HISTORY, true),
            syncBookmarks = p.getBoolean(KEY_SYNC_BOOKMARKS, true),
            syncSettings = p.getBoolean(KEY_SYNC_SETTINGS, true),
            syncAiSpaces = p.getBoolean(KEY_SYNC_AI, true),
            lastSyncTimeMs = p.getLong(KEY_LAST_SYNC, 0L)
        )
        _state.value = loaded
        if (loaded.isSignedIn) {
            scope.launch {
                loadCachedAvatarOrFetch(loaded.photoUrl)
            }
        }
    }

    private fun save(next: GoogleAccountState) {
        _state.value = next
        prefs?.edit()
            ?.putBoolean(KEY_SIGNED_IN, next.isSignedIn)
            ?.putString(KEY_NAME, next.displayName)
            ?.putString(KEY_EMAIL, next.email)
            ?.putString(KEY_PHOTO_URL, next.photoUrl)
            ?.putString(KEY_WEB_CLIENT_ID, next.oauthWebClientId)
            ?.putString(KEY_AUTH_MODE, next.authMode)
            ?.putBoolean(KEY_SYNC_HISTORY, next.syncHistoryAndTabs)
            ?.putBoolean(KEY_SYNC_BOOKMARKS, next.syncBookmarks)
            ?.putBoolean(KEY_SYNC_SETTINGS, next.syncSettings)
            ?.putBoolean(KEY_SYNC_AI, next.syncAiSpaces)
            ?.putLong(KEY_LAST_SYNC, next.lastSyncTimeMs)
            ?.apply()
    }

    fun updateOAuthWebClientId(clientId: String) {
        save(_state.value.copy(oauthWebClientId = clientId.trim()))
    }

    fun updateSyncOptions(
        historyAndTabs: Boolean = _state.value.syncHistoryAndTabs,
        bookmarks: Boolean = _state.value.syncBookmarks,
        settings: Boolean = _state.value.syncSettings,
        aiSpaces: Boolean = _state.value.syncAiSpaces
    ) {
        save(
            _state.value.copy(
                syncHistoryAndTabs = historyAndTabs,
                syncBookmarks = bookmarks,
                syncSettings = settings,
                syncAiSpaces = aiSpaces
            )
        )
    }

    suspend fun signInWithCredentialManager(
        activity: Activity,
        webClientId: String = _state.value.oauthWebClientId
    ): Result<GoogleAccountState> {
        val cid = webClientId.trim()
        if (cid.isBlank()) {
            return Result.failure(IllegalArgumentException("Google Cloud Web Client ID를 먼저 입력하거나, 브라우저 Google 세션 연동을 사용해 주세요."))
        }
        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(cid)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activity, request)
            val cred = response.credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val email = googleCred.id
                val name = googleCred.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@")
                val photo = googleCred.profilePictureUri?.toString().orEmpty()
                val next = _state.value.copy(
                    isSignedIn = true,
                    displayName = name,
                    email = email,
                    photoUrl = photo,
                    oauthWebClientId = cid,
                    authMode = "CREDENTIAL_MANAGER",
                    lastSyncTimeMs = System.currentTimeMillis()
                )
                save(next)
                scope.launch { fetchAndCacheAvatar(photo) }
                next
            } else {
                throw IllegalStateException("Google 계정 자격 증명을 확인할 수 없습니다.")
            }
        }
    }

    fun applyWebSessionProfile(displayName: String, email: String, photoUrl: String) {
        val cleanEmail = email.trim()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@").ifBlank { "Google 사용자" } }
        val cleanPhoto = photoUrl.trim()
        val next = _state.value.copy(
            isSignedIn = true,
            displayName = cleanName,
            email = cleanEmail,
            photoUrl = cleanPhoto,
            authMode = "WEB_SESSION",
            lastSyncTimeMs = System.currentTimeMillis()
        )
        save(next)
        if (cleanPhoto.isNotBlank()) {
            scope.launch { fetchAndCacheAvatar(cleanPhoto) }
        }
    }

    fun extractProfileFromWebView(webView: WebView, onResult: (Boolean, String) -> Unit) {
        val js = """
            (function() {
                try {
                    var result = { name: '', email: '', photo: '' };
                    var imgs = document.querySelectorAll('img[src*="googleusercontent.com"]');
                    for (var i = 0; i < imgs.length; i++) {
                        var src = imgs[i].getAttribute('src') || '';
                        if (src.indexOf('lh3.googleusercontent.com') !== -1 || src.indexOf('googleusercontent.com/a/') !== -1) {
                            result.photo = src.replace(/=s\d+-c/g, '=s192-c');
                            var alt = imgs[i].getAttribute('alt') || '';
                            if (alt && !result.name && alt.indexOf('Google') === -1) result.name = alt;
                            break;
                        }
                    }
                    var links = document.querySelectorAll('a[aria-label*="@"], button[aria-label*="@"], div[aria-label*="@"]');
                    for (var j = 0; j < links.length; j++) {
                        var label = links[j].getAttribute('aria-label') || '';
                        var emailMatch = label.match(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/);
                        if (emailMatch) {
                            result.email = emailMatch[0];
                            var paren = label.replace(emailMatch[0], '').replace(/Google\s*계정:?/gi, '').replace(/Google\s*Account:?/gi, '').replace(/[()]/g, '').trim();
                            if (paren && !result.name) result.name = paren;
                            break;
                        }
                    }
                    if (!result.email) {
                        var text = (document.body && document.body.innerText) ? document.body.innerText : '';
                        var m = text.match(/[A-Za-z0-9._%+-]+@gmail\.com/i);
                        if (m) result.email = m[0];
                    }
                    return JSON.stringify(result);
                } catch (e) {
                    return JSON.stringify({ error: String(e) });
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { raw ->
            try {
                if (raw.isNullOrBlank() || raw == "null") {
                    onResult(false, "Google 페이지에 로그인된 계정을 찾지 못했습니다.")
                    return@evaluateJavascript
                }
                val unescaped = JSONObject("{"v":$raw}").getString("v")
                val obj = JSONObject(unescaped)
                val email = obj.optString("email", "")
                val name = obj.optString("name", "")
                val photo = obj.optString("photo", "")
                if (email.isBlank() && photo.isBlank()) {
                    onResult(false, "Google에 로그인된 탭에서 다시 시도해 주세요.")
                } else {
                    applyWebSessionProfile(name, email, photo)
                    onResult(true, "Google 프로필 연동 완료 (${email.ifBlank { name }})")
                }
            } catch (_: Throwable) {
                onResult(false, "프로필 정보를 읽어오지 못했습니다.")
            }
        }
    }

    suspend fun signOut(activity: Activity?) {
        if (activity != null) {
            runCatching {
                CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            }
        }
        _avatarBitmap.value = null
        appContext?.let { File(it.filesDir, "google_avatar.png").delete() }
        save(
            _state.value.copy(
                isSignedIn = false,
                displayName = "",
                email = "",
                photoUrl = "",
                authMode = ""
            )
        )
    }

    private suspend fun loadCachedAvatarOrFetch(photoUrl: String) {
        val ctx = appContext ?: return
        val file = File(ctx.filesDir, "google_avatar.png")
        if (file.exists()) {
            val cached = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            if (cached != null) {
                _avatarBitmap.value = cached
            }
        }
        if (photoUrl.isNotBlank()) {
            fetchAndCacheAvatar(photoUrl)
        }
    }

    private suspend fun fetchAndCacheAvatar(photoUrl: String) = withContext(Dispatchers.IO) {
        if (photoUrl.isBlank()) return@withContext
        val ctx = appContext ?: return@withContext
        runCatching {
            val conn = (URL(photoUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            conn.inputStream.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) {
                    _avatarBitmap.value = bmp
                    val outFile = File(ctx.filesDir, "google_avatar.png")
                    FileOutputStream(outFile).use { fos ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                    }
                }
            }
            conn.disconnect()
        }
    }
}
