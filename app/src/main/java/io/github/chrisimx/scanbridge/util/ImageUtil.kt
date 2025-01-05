package io.github.chrisimx.scanbridge.util

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.rotateBy90(): Bitmap {
    val matrix = Matrix().apply { postRotate(90F) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
