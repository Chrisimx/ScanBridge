package io.github.chrisimx.scanbridge.export

import io.github.vinceglb.filekit.PlatformFile

interface ExportFileManager {
    fun createFile(extension: String): PlatformFile
}
