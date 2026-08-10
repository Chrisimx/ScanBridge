package io.github.chrisimx.scanbridge.cropfeature

import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.model.ScanBridgeFile

interface ImageCropService {
    suspend fun crop(
        sourcePath: ScanBridgeFile,
        outputPath: ScanBridgeFile,
        cropRect: Rect,
    ): Boolean
}
