package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.filesystem.StandardDirectoryProvider
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div

class DefaultExportDirectoryProvider(
    standardDirectoryProvider: StandardDirectoryProvider
) : ExportDirectoryProvider {
    override val exportDirectory =
        standardDirectoryProvider.appFilesDirectory / "export"
}
