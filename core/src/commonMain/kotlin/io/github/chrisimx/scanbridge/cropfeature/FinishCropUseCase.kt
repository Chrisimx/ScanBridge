package io.github.chrisimx.scanbridge.cropfeature

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.filesystem.FileSystem
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import io.github.chrisimx.scanbridge.util.getEditedImageName

class FinishCropUseCase(
    private val imageCropService: ImageCropService,
    scanBridgeDb: ScanBridgeDb,
    private val fileSystem: FileSystem
) {
    val scannedPageDao = scanBridgeDb.scannedPageDao()

    suspend operator fun invoke(
        page: ScannedPage,
        cropRect: Rect,
    ): Boolean {
        val scanFile = ScanBridgeFile(page.filePath)

        // Determine an output for the cropped image
        val editFileName = scanFile.getEditedImageName()
        val editFile = scanFile
            .parent()
            ?.child(editFileName)
            ?: return false

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
