package io.github.chrisimx.scanbridge.util

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import javax.imageio.ImageIO
import kotlin.text.iterator
import kotlin.use
import kotlin.uuid.Uuid
import kotlinx.io.Buffer
import kotlinx.io.asInputStream
import kotlinx.io.write

actual fun extractPdfImages(
    pdf: PlatformFile,
    outputDir: PlatformFile
): List<String> {
    val pdfDoc = PdfDocument(PdfReader(pdf.file))
    var imageCounter = 1

    val savedImages = mutableListOf<String>()

    for (i in 1..pdfDoc.numberOfPages) {
        val page = pdfDoc.getPage(i)
        val xObjects = page.resources.getResource(PdfName.XObject) ?: continue

        for (key in xObjects.keySet()) {
            val obj = xObjects.getAsStream(key) ?: continue
            val subtype = obj.getAsName(PdfName.Subtype)
            if (subtype == PdfName.Image) {
                val imageXObject = PdfImageXObject(obj)
                val bytes = imageXObject.imageBytes

                val imageByteBuffer = Buffer()
                imageByteBuffer.write(bytes)

                // Convert raw bytes to Bitmap
                val image = runCatching {
                    imageByteBuffer.asInputStream().use {
                        ImageIO.read(it)
                    }
                }.getOrNullIgnoreCancellation()

                if (image != null) {
                    val outputFile = PlatformFile(outputDir, "scan-${Uuid.random()}.png")
                    // Save bitmap as PNG
                    ImageIO.write(image, "png", outputFile.file)
                    savedImages.add(outputFile.absolutePath())
                    imageCounter++
                }
            }
        }
    }

    pdfDoc.close()

    return savedImages
}
