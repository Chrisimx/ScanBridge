package io.github.chrisimx.scanbridge.imagerotation

import io.github.vinceglb.filekit.PlatformFile

interface ImageRotationService {
    suspend fun rotate90ToRight(
        sourceFile: PlatformFile,
        outputFile: PlatformFile,
    ): Boolean
}
