package io.github.chrisimx.scanbridge.services

import android.net.Uri

interface DebugLogService {
    fun clear()
    fun saveToFile(uri: Uri)

    fun flush()
}
