package io.github.chrisimx.scanbridge.export

import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.vinceglb.filekit.PlatformFile

open class NoopExportModule(
    override val type: ExportModuleType
) : ExportModule {
    override suspend fun export(pagesToExport: List<ScannedPage>): PlatformFile {
        throw NotImplementedError("No-op export module")
    }
}
