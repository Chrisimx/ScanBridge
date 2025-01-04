package io.github.chrisimx.scanbridge.util

import java.io.File

fun doTempFilesExist(rootDir: File): Boolean {
    if (!rootDir.exists()) {
        return false
    }
    val scanTempFileExists = rootDir.listFiles()?.any { it.name.startsWith("scan") } == true

    val exportDir = File(rootDir, "exports")

    if (!exportDir.exists()) {
        return scanTempFileExists
    }

    val pdfExportTempFileExists =
        exportDir.listFiles()?.any { it.name.startsWith("pdfexport") } == true
    return scanTempFileExists || pdfExportTempFileExists
}