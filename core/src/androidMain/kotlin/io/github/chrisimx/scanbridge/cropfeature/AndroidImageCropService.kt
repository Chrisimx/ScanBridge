package io.github.chrisimx.scanbridge.cropfeature

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.chrisimx.scanbridge.cropfeature.ImageCropService
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidImageCropService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageCropService {
    private val logger = loggerFactory.withClass(this::class)

    override suspend fun crop(sourcePath: ScanBridgeFile, outputPath: ScanBridgeFile, cropRect: Rect): Boolean = withContext(Dispatchers.IO) {
        val pathString = sourcePath.path.toString()
        val sourceBitmap = BitmapFactory.decodeFile(pathString)
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

        val croppedFile = File(outputPath.path.toString())
        croppedFile.outputStream().use {
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        croppedBitmap.recycle()

        return@withContext true
    }
}
