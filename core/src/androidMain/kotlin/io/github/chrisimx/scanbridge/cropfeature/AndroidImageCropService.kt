package io.github.chrisimx.scanbridge.cropfeature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered

class AndroidImageCropService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageCropService {
    private val logger = loggerFactory.withClass(this::class)

    override suspend fun crop(sourceFile: PlatformFile, outputFile: PlatformFile, cropRect: Rect): Boolean = withContext(Dispatchers.IO) {
        val sourceFileInputStream = sourceFile
            .source()
            .buffered()
            .asInputStream()

        val sourceBitmap = sourceFileInputStream.use {
            BitmapFactory.decodeStream(it)
        }
        if (sourceBitmap == null) {
            logger.error {
                "Could not decode source bitmap for cropping"
            }
            return@withContext false
        }

        val startX = cropRect.left * sourceBitmap.width
        val startY = cropRect.top * sourceBitmap.height

        val width = (cropRect.right - cropRect.left) * sourceBitmap.width
        val height = (cropRect.bottom - cropRect.top) * sourceBitmap.height

        val croppedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            startX.roundToInt(),
            startY.roundToInt(),
            width.roundToInt(),
            height.roundToInt()
        )
        sourceBitmap.recycle()

        val outputFileOutputStream = outputFile
            .sink(append = false)
            .buffered()
            .asOutputStream()

        outputFileOutputStream.use {
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        croppedBitmap.recycle()

        return@withContext true
    }
}
