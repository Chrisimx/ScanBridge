import io.github.chrisimx.scanbridge.cropfeature.ImageCropService
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.model.Rect
import io.github.chrisimx.scanbridge.util.getOrNullIgnoreCancellation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered

class JvmImageCropService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageCropService {
    private val logger = loggerFactory.withClass(this::class)

    override suspend fun crop(sourceFile: PlatformFile, outputFile: PlatformFile, cropRect: Rect): Boolean = withContext(Dispatchers.IO) {
        val sourceBitmap = runCatching {
            ImageIO.read(sourceFile.file)
        }.getOrNullIgnoreCancellation()

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

        val croppedImage = sourceBitmap.getSubimage(
            startX.roundToInt(),
            startY.roundToInt(),
            width.roundToInt(),
            height.roundToInt()
        )

        ImageIO.write(croppedImage, "jpg", outputFile.file)

        return@withContext true
    }
}
