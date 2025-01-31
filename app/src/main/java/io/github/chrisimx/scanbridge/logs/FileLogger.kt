package io.github.chrisimx.scanbridge.logs

import java.io.BufferedWriter
import java.time.format.DateTimeFormatter
import timber.log.Timber

class FileLogger(val output: BufferedWriter) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val date = DateTimeFormatter.ISO_DATE_TIME.format(java.time.LocalDateTime.now())
        try {
            output.write("$date - $priority - $tag - $message - $t\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
