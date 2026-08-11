package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.model.ScanBridgeFile

open class NoopExportModule(
    override val type: ExportModuleType
) : ExportModule {
    override suspend fun export(pagesToExport: List<ScannedPage>): ScanBridgeFile {
        throw NotImplementedError("No-op export module")
    }
}

class PdfNoopExportModule : NoopExportModule(ExportModuleType.PDF)
