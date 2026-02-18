package io.github.chrisimx.scanbridge.util

import java.io.File

fun File.extractBaseFilename(): String {
    val regex = Regex("^scan-[a-f0-9-]+")
    return regex.find(this.name)!!.value
}

fun File.getEditedImageName(): String {
    val baseName = extractBaseFilename()

    return "$baseName edit-${System.currentTimeMillis()}.jpg"
}
