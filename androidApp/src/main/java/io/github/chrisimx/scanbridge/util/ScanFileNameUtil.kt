package io.github.chrisimx.scanbridge.util

import java.io.File

fun File.getEditedImageName(): String {
    val baseName = this.name.substringBefore(" edit-")

    return "$baseName edit-${System.currentTimeMillis()}"
}
