package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.vinceglb.filekit.PlatformFile

interface ExportModule {
    val type: ExportModuleType

    suspend fun export(pagesToExport: List<ScannedPage>): PlatformFile
}
