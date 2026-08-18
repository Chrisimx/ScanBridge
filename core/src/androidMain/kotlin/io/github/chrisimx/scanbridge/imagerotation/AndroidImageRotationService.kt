package io.github.chrisimx.scanbridge.imagerotation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered

class AndroidImageRotationService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageRotationService {
    val logger = loggerFactory.withClass(this::class)

    override suspend fun rotate90ToRight(
        sourceFile: PlatformFile,
        outputFile: PlatformFile
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceFileInputStream = sourceFile
            .source()
            .buffered()
            .asInputStream()

        val sourceBitmap = sourceFileInputStream.use {
            BitmapFactory.decodeStream(it)
        }
        if (sourceBitmap == null) {
            logger.error {
                "Could not decode source bitmap for rotating"
            }
            return@withContext false
        }

        val matrix = Matrix().apply { postRotate(90F) }
        val rotatedBitmap =  Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)

        sourceBitmap.recycle()

        val rotatedFileOutputStream = outputFile
            .sink(append = false)
            .buffered()
            .asOutputStream()

        rotatedFileOutputStream.use {
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        rotatedBitmap.recycle()

        return@withContext true
    }

}
