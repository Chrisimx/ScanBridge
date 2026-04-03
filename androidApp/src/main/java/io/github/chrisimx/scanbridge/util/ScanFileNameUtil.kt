package io.github.chrisimx.scanbridge.util

import java.io.File

fun File.getEditedImageName(): String {
    val baseName = this.nameWithoutExtension
    val extension = this.extension

    return "$baseName edit-${System.currentTimeMillis()}.$extension"
}
