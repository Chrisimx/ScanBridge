package io.github.chrisimx.scanbridge

import android.content.Context
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CrashHandler(
    val context: Context
) : Thread.UncaughtExceptionHandler {
    private val TAG = "CrashHandler"
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "Uncaught exception", e)

        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val dateTime = LocalDateTime.now().format(format)

        val crashDir = File(context.filesDir, "crashes")
        if (!crashDir.exists()) {
            if (!crashDir.mkdirs()) {
                Log.e(TAG, "Couldn't create crash directory")
                File(context.filesDir, "crash-${dateTime}.log").writeText(e.stackTraceToString())
                return
            }
        }
        File(crashDir, "crash-${dateTime}.log").writeText(e.stackTraceToString())

        defaultHandler?.uncaughtException(t, e)
    }
}