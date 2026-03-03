package io.github.chrisimx.scanbridge.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.uuid.Uuid

fun extractPdfImages(pdfPath: String, outputDir: File): List<String> {
    val pdfDoc = PdfDocument(PdfReader(pdfPath))
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

                // Convert raw bytes to Bitmap
                val outputFile = File(outputDir, "image_${Uuid.random()}_$imageCounter.png")
                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    // Save bitmap as PNG
                    FileOutputStream(outputFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                    savedImages.add(outputFile.absolutePath)
                    imageCounter++
                }
            }
        }
    }

    pdfDoc.close()
    Files.deleteIfExists(Path(pdfPath))
    return savedImages
}
