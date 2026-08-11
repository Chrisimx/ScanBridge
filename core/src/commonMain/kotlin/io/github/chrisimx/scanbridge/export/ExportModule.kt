package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.model.ScanBridgeFile

interface ExportModule {
    val type: ExportModuleType

    suspend fun export(pagesToExport: List<ScannedPage>): ScanBridgeFile
}
