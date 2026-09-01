package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.UniformTypeIdentifiers.UTTypePNG
import kotlin.uuid.Uuid
import platform.CoreFoundation.CFURLRef

@OptIn(ExperimentalForeignApi::class)
actual fun extractPdfImages(
    pdf: PlatformFile,
    outputDir: PlatformFile
): List<String> {
    TODO()
}
