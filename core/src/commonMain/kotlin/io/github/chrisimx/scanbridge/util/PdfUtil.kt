package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile

expect fun extractPdfImages(pdf: PlatformFile, outputDir: PlatformFile): List<String>
