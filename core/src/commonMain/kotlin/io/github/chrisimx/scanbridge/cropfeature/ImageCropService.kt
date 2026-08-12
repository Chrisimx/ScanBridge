package io.github.chrisimx.scanbridge.cropfeature

import io.github.chrisimx.scanbridge.model.Rect
import io.github.vinceglb.filekit.PlatformFile

interface ImageCropService {
    suspend fun crop(
        sourceFile: PlatformFile,
        outputFile: PlatformFile,
        cropRect: Rect,
    ): Boolean
}
