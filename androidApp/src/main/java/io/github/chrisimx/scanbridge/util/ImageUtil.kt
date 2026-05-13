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

fun Bitmap.cropWithRect(cropRect: Rect): Bitmap {
    val startX = cropRect.left * this.width
    val startY = cropRect.top * this.height

    val width = (cropRect.right - cropRect.left) * this.width
    val height = (cropRect.bottom - cropRect.top) * this.height

    return Bitmap.createBitmap(
        this,
        startX.roundToInt(),
        startY.roundToInt(),
        width.roundToInt(),
        height.roundToInt()
    )
}

fun Bitmap.saveAsJPEG(file: File) {
    file.outputStream().use {
        this.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
}
