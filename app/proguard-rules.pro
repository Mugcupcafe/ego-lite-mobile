# Ego Lite Mobile — Release R8 / ProGuard Rules

# 1. App Models, MCP & JS Bridges
-keep class com.egolite.mobile.model.** { *; }
-keep class com.egolite.mobile.mcp.** { *; }
-keep class com.egolite.mobile.account.** { *; }
-keep class com.egolite.mobile.nativebridge.** { *; }

# 2. WebView @JavascriptInterface (CRITICAL: prevent R8 stripping JS callbacks)
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 3. kotlinx.serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep,includedescriptorclasses class com.egolite.mobile.**$$serializer { *; }
-keepclassmembers class com.egolite.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.egolite.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 4. Ktor CIO Server & Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**
-dontwarn kotlinx.atomicfu.**

# 5. Google ML Kit & Credential Manager
-keep class com.google.mlkit.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
