import io.github.chrisimx.scanbridge.imagerotation.ImageRotationService
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.vinceglb.filekit.PlatformFile
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopJvmImageRotationService(
    loggerFactory: ScanBridgeLoggerFactory
) : ImageRotationService {
    val logger = loggerFactory.withClass(this::class)

    override suspend fun rotate90ToRight(
        sourceFile: PlatformFile,
        outputFile: PlatformFile
    ): Boolean = withContext(Dispatchers.IO) {

        try {
            val src = ImageIO.read(sourceFile.file)

            val dst = BufferedImage(
                src.height,
                src.width,
                src.type
            )

            val pixels = IntArray(src.width * src.height)
            src.getRGB(
                0, 0,
                src.width,
                src.height,
                pixels,
                0,
                src.width
            )

            for (y in 0 until src.height) {
                for (x in 0 until src.width) {
                    dst.setRGB(
                        src.height - 1 - y,
                        x,
                        pixels[y * src.width + x]
                    )
                }
            }

            ImageIO.write(dst, "jpg", outputFile.file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error { "Error while rotating image 90 degrees to right: $e" }
            return@withContext false
        }

        return@withContext true
    }
}
