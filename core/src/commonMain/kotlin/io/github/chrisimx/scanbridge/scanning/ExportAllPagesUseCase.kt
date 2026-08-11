package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.export.ExportModuleManager
import io.github.chrisimx.scanbridge.export.ExportModuleType
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException


class ExportAllPagesUseCase(
    val exportModuleManager: ExportModuleManager,
    scanBridgeDb: ScanBridgeDb
) {
    val scannedPageDao = scanBridgeDb.scannedPageDao()

    suspend operator fun invoke(exportModuleType: ExportModuleType, sessionId: Uuid): ExportAllPagesResult {
        val exportModule = exportModuleManager.getExportModuleByType(exportModuleType)
            ?: return ExportAllPagesResult.ExportModuleNotFound

        val scannedPages = scannedPageDao.getAllForSession(sessionId)

        return try {
            val exportFile = exportModule.export(scannedPages)
             ExportAllPagesResult.Success(exportFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExportAllPagesResult.ExportFailed(e)
        }
    }
}

sealed class ExportAllPagesResult {
    data class Success(val exportedFile: ScanBridgeFile) : ExportAllPagesResult()
    data class ExportFailed(val error: Throwable) : ExportAllPagesResult()
    object ExportModuleNotFound : ExportAllPagesResult()
}
