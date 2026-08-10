package io.github.chrisimx.scanbridge.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

actual fun String.toClipEntry(): ClipEntry {
    return ClipData.newPlainText(this, this).toClipEntry()
}
