package io.github.chrisimx.scanbridge.cropfeature

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.util.getEditedFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.path

class FinishCropUseCase(
    private val imageCropService: ImageCropService,
    scanBridgeDb: ScanBridgeDb,
    loggerFactory: ScanBridgeLoggerFactory
) {
    private val scannedPageDao = scanBridgeDb.scannedPageDao()
    private val logger = loggerFactory.withClass(this::class)

    suspend operator fun invoke(
        page: ScannedPage,
        cropRect: Rect,
    ): Boolean {
        val scanFile = PlatformFile(page.filePath)

        // Determine an output for the cropped image
        val editFile = scanFile.getEditedFile()

        if (editFile == null) {
            logger.error { "Could not determine output file name for cropped image" }
            return false
        }

        val successfulCrop = imageCropService.crop(
            sourceFile = scanFile,
            outputFile = editFile,
            cropRect = cropRect,
        )

        if (!successfulCrop) {
            return false
        }

        scannedPageDao.update(
            page.copy(
                filePath = editFile.path
            )
        )

        // Delete the original file
        scanFile.delete()

        return true
    }
}
