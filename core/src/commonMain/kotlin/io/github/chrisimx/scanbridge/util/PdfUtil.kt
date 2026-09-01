package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid

expect fun extractPdfImages(pdf: PlatformFile, outputDir: PlatformFile): List<String>
