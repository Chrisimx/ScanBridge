package io.github.chrisimx.scanbridge.util

import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import kotlin.time.Clock

fun ScanBridgeFile.getEditedImageName(): String {
    val name = this.path.name
    val baseName = name.substringBefore(" edit-")

    return "$baseName edit-${Clock.System.now().toEpochMilliseconds()}"
}
