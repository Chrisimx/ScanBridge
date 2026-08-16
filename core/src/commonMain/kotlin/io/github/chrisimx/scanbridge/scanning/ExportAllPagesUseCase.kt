package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.TempFile
import io.github.chrisimx.scanbridge.export.ExportModuleManager
import io.github.chrisimx.scanbridge.export.ExportModuleType
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException


class ExportAllPagesUseCase(
    val exportModuleManager: ExportModuleManager,
    scanBridgeDb: ScanBridgeDb
) {
    val scannedPageDao = scanBridgeDb.scannedPageDao()
    val tmpFileDao = scanBridgeDb.tmpFileDao()

    suspend operator fun invoke(exportModuleType: ExportModuleType, sessionId: Uuid): ExportAllPagesResult {
        val exportModule = exportModuleManager.getExportModuleByType(exportModuleType)
            ?: return ExportAllPagesResult.ExportModuleNotFound

        val scannedPages = scannedPageDao.getAllForSession(sessionId)

        if (scannedPages.isEmpty()) {
            return ExportAllPagesResult.NoPages
        }

        return try {
            val exportFile = exportModule.export(scannedPages)
            tmpFileDao.insertAll(
                TempFile(ownerSessionId =  sessionId, path = exportFile.path)
            )
            ExportAllPagesResult.Success(exportFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExportAllPagesResult.ExportFailed(e)
        }
    }
}

sealed class ExportAllPagesResult {
    data class Success(val exportedFile: PlatformFile) : ExportAllPagesResult()
    data class ExportFailed(val error: Throwable) : ExportAllPagesResult()
    data object ExportModuleNotFound : ExportAllPagesResult()
    data object NoPages : ExportAllPagesResult()
}
