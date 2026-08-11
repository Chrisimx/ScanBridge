package io.github.chrisimx.scanbridge.imagerotation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidImageRotationService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageRotationService {
    val logger = loggerFactory.withClass(this::class)

    override suspend fun rotate90ToRight(
        sourcePath: ScanBridgeFile,
        outputPath: ScanBridgeFile
    ): Boolean = withContext(Dispatchers.IO) {
        val pathString = sourcePath.path.toString()
        val sourceBitmap = BitmapFactory.decodeFile(pathString)
        if (sourceBitmap == null) {
            logger.error {
                "Could not decode source bitmap for rotating"
            }
            return@withContext false
        }

        val matrix = Matrix().apply { postRotate(90F) }
        val rotatedBitmap =  Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)

        sourceBitmap.recycle()

        val rotatedFile = File(outputPath.path.toString())
        rotatedFile.outputStream().use {
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        rotatedBitmap.recycle()

        return@withContext true
    }

}
