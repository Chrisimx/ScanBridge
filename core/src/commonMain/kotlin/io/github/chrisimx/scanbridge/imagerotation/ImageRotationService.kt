package io.github.chrisimx.scanbridge.imagerotation

import io.github.chrisimx.scanbridge.model.ScanBridgeFile

interface ImageRotationService {
    suspend fun rotate90ToRight(
        sourcePath: ScanBridgeFile,
        outputPath: ScanBridgeFile,
    ): Boolean
}
