package io.github.chrisimx.scanbridge.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent
import kotlin.time.Clock

fun PlatformFile.getEditedFile(): PlatformFile? {
    val name = this.name
    val baseName = name.substringBefore(" edit-")
    val editName = "$baseName edit-${Clock.System.now().toEpochMilliseconds()}"

    return this.parent()?.let {
        PlatformFile(it, editName)
    }
}
