package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.shareFile

actual suspend fun platformShareFile(file: PlatformFile) {
    FileKit.shareFile(file)
}
