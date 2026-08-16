package io.github.chrisimx.scanbridge.export

import io.github.vinceglb.filekit.PlatformFile

interface ExportDirectoryProvider {
    val exportDirectory: PlatformFile
}
