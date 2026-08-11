package io.github.chrisimx.scanbridge.cropfeature

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.filesystem.FileSystem
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.util.getEditedFile

class FinishCropUseCase(
    private val imageCropService: ImageCropService,
    scanBridgeDb: ScanBridgeDb,
    private val fileSystem: FileSystem,
    loggerFactory: ScanBridgeLoggerFactory
) {
    private val scannedPageDao = scanBridgeDb.scannedPageDao()
    private val logger = loggerFactory.withClass(this::class)

    suspend operator fun invoke(
        page: ScannedPage,
        cropRect: Rect,
    ): Boolean {
        val scanFile = ScanBridgeFile(page.filePath)

        // Determine an output for the cropped image
        val editFile = scanFile.getEditedFile()

        if (editFile == null) {
            logger.error { "Could not determine output file name for cropped image" }
            return false
        }

        val successfulCrop = imageCropService.crop(
            sourcePath = scanFile,
            outputPath = editFile,
            cropRect = cropRect,
        )

        if (!successfulCrop) {
            return false
        }

        scannedPageDao.update(
            page.copy(
                filePath = editFile.path.toString()
            )
        )

        // Delete the original file
        fileSystem.delete(scanFile)

        return true
    }
}
