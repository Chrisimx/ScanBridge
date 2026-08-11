package io.github.chrisimx.scanbridge.util

import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import kotlin.time.Clock

fun ScanBridgeFile.getEditedFile(): ScanBridgeFile? {
    val name = this.path.name
    val baseName = name.substringBefore(" edit-")
    val editName = "$baseName edit-${Clock.System.now().toEpochMilliseconds()}"

    return this
        .parent()
        ?.child(editName)
}
