package io.github.chrisimx.scanbridge.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import java.io.File
import kotlin.math.roundToInt

fun Bitmap.saveAsJPEG(file: File) {
    file.outputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
}
