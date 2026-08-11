package io.github.chrisimx.scanbridge.imagerotation

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.filesystem.FileSystem
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.util.getEditedFile
import kotlin.uuid.Uuid

class RotateScanUseCase(
    scanBridgeDb: ScanBridgeDb,
    loggerFactory: ScanBridgeLoggerFactory,
    val fileSystem: FileSystem,
    val imageRotationService: ImageRotationService
) {
    private val scannedPageDao = scanBridgeDb.scannedPageDao()
    private val logger = loggerFactory.withClass(this::class)

    suspend fun rotateScan(scannedPageId: Uuid) {
        val scannedPage = scannedPageDao.getByScanId(scannedPageId) ?: return
        val scanFile = ScanBridgeFile(scannedPage.filePath)
        val editFile = scanFile.getEditedFile()

        if (editFile == null) {
            logger.error { "Could not determine output file name for rotated image" }
            return
        }

        val successRotating = imageRotationService.rotate90ToRight(scanFile, editFile)

        if (!successRotating) {
            return
        }

        scannedPageDao.update(
            scannedPage.copy(
                filePath = editFile.path.toString()
            )
        )

        fileSystem.delete(scanFile)
    }
}
