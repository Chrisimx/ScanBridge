package io.github.chrisimx.scanbridge.util

import android.graphics.Bitmap
import java.io.File

fun Bitmap.saveAsJPEG(file: File) {
    file.outputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
}
