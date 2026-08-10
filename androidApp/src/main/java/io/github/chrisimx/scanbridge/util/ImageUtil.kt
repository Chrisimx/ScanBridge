package io.github.chrisimx.scanbridge.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import java.io.File
import kotlin.math.roundToInt

fun Bitmap.rotateBy90(): Bitmap {
    val matrix = Matrix().apply { postRotate(90F) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.saveAsJPEG(file: File) {
    file.outputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
}
