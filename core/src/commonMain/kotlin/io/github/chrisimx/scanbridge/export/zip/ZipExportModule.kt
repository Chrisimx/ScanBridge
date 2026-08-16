package io.github.chrisimx.scanbridge.export.zip

import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.export.ExportDirectoryProvider
import io.github.chrisimx.scanbridge.export.ExportFileManager
import io.github.chrisimx.scanbridge.export.ExportModule
import io.github.chrisimx.scanbridge.export.ExportModuleType
import io.github.chrisimx.scanbridge.model.FileToBeExported
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

class ZipExportModule(
    val zipService: ZipService,
    val exportFileManager: ExportFileManager
) : ExportModule {
    override val type: ExportModuleType = ExportModuleType.ZIP

    override suspend fun export(pagesToExport: List<ScannedPage>): PlatformFile {
        val filesToBeExported = pagesToExport.map {
            val pageFile = PlatformFile(it.filePath)
            FileToBeExported(
                pageFile,
                it.outputName ?: pageFile.name
            )
        }

        val exportFile = exportFileManager.createFile("zip")
        zipService.zipFlat(filesToBeExported, exportFile)
        return exportFile
    }

}
