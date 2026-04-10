package io.github.chrisimx.scanbridge.util

import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val OCR_TARGET_MAX_SIDE_PX = 4000

data class RecognizedOcrLine(
    val text: String,
    val leftRatio: Float,
    val bottomRatio: Float,
    val heightRatio: Float,
    val widthRatio: Float
)

data class RecognizedOcrPage(
    val lines: List<RecognizedOcrLine>
)

suspend fun recognizePageText(file: File, recognizer: TextRecognizer): RecognizedOcrPage = withContext(Dispatchers.Default) {
    val boundsOnlyOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, boundsOnlyOptions)

    val sampledBitmapOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateOcrSampleSize(
            boundsOnlyOptions.outWidth,
            boundsOnlyOptions.outHeight
        )
    }

    val bitmap = BitmapFactory.decodeFile(file.absolutePath, sampledBitmapOptions)
        ?: return@withContext RecognizedOcrPage(emptyList())

    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizedText = recognizer.process(image).await()
        val width = bitmap.width.toFloat().coerceAtLeast(1f)
        val height = bitmap.height.toFloat().coerceAtLeast(1f)

        RecognizedOcrPage(
            recognizedText.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val boundingBox = line.boundingBox ?: return@mapNotNull null
                    val trimmedText = line.text.trim()
                    if (trimmedText.isEmpty()) {
                        return@mapNotNull null
                    }

                    RecognizedOcrLine(
                        text = trimmedText,
                        leftRatio = boundingBox.left / width,
                        bottomRatio = (height - boundingBox.bottom) / height,
                        heightRatio = boundingBox.height() / height,
                        widthRatio = boundingBox.width() / width
                    )
                }
            }
        )
    } finally {
        bitmap.recycle()
    }
}

private fun calculateOcrSampleSize(width: Int, height: Int): Int {
    val longestSide = maxOf(width, height)
    if (longestSide <= OCR_TARGET_MAX_SIDE_PX || longestSide <= 0) {
        return 1
    }

    var sampleSize = 1
    while ((longestSide / sampleSize) > OCR_TARGET_MAX_SIDE_PX) {
        sampleSize *= 2
    }

    return sampleSize
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
