package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*

@OptIn(ExperimentalForeignApi::class)
actual fun extractPdfImages(pdf: PlatformFile, outputDir: PlatformFile): List<String> {
    TODO()
}
