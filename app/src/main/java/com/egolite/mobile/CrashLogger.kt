package com.egolite.mobile

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {
    private const val LAST_CRASH_FILE = "last_crash.txt"
    private const val CRASH_DIR_NAME = "crash_logs"
    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appCtx, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val now = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val report = buildString {
            appendLine("=== ego-lite-mobile Crash Report ===")
            appendLine("Time: $now")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(sw.toString())
        }

        // 1) Internal app-private file (always writable, shown on next app launch)
        runCatching {
            File(context.filesDir, LAST_CRASH_FILE).writeText(report, Charsets.UTF_8)
        }

        // 2) External app-specific dir (accessible via file manager under Android/data/.../files/crash_logs)
        runCatching {
            val dir = File(context.getExternalFilesDir(null), CRASH_DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            File(dir, "crash_$now.txt").writeText(report, Charsets.UTF_8)
            File(dir, LAST_CRASH_FILE).writeText(report, Charsets.UTF_8)
        }
    }

    fun readLastCrash(context: Context): String? {
        val file = File(context.filesDir, LAST_CRASH_FILE)
        return if (file.exists()) runCatching { file.readText(Charsets.UTF_8) }.getOrNull()?.takeIf { it.isNotBlank() } else null
    }

    fun clearLastCrash(context: Context) {
        runCatching { File(context.filesDir, LAST_CRASH_FILE).delete() }
    }

    fun getExternalCrashDirPath(context: Context): String {
        val dir = File(context.getExternalFilesDir(null), CRASH_DIR_NAME)
        return dir.absolutePath
    }
}
