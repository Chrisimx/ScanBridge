package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile

actual suspend fun platformShareFile(file: PlatformFile) {
    throw NotImplementedError("Desktop does not support sharing files")
}
