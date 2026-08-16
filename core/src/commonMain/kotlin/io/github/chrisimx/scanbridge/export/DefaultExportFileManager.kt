package io.github.chrisimx.scanbridge.export

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

class DefaultExportFileManager(
    val exportDirectoryProvider: ExportDirectoryProvider
) : ExportFileManager {
    override fun createFile(extension: String): PlatformFile {
        val exportDirectory = exportDirectoryProvider.exportDirectory
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedDateTime = localDateTime.format(LocalDateTime.Formats.ISO)

        val exportFileName = "export-$formattedDateTime.$extension"

        exportDirectory.createDirectories()

        return exportDirectory / exportFileName
    }
}
