package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.Millimeters
import io.github.chrisimx.esclkt.millimeters

// Paper format class
data class PaperFormat(val name: String, val width: LengthUnit, val height: LengthUnit)

fun loadDefaultFormats(): List<PaperFormat> {
    val defaultPaperFormats: MutableList<PaperFormat> = mutableListOf()

    // calculate the width and height of the ISO 216 paper formats based on the A0 format
    val a0 = PaperFormat("A0", width = 841.0.millimeters(), height = 1189.0.millimeters())
    defaultPaperFormats.add(a0)

    // Generate A1 to A10 formats
    var currentWidth = a0.width.toMillimeters()
    var currentHeight = a0.height.toMillimeters()

    for (i in 1..10) {
        // Swap dimensions (width becomes half of height)
        val newWidth = Millimeters(currentHeight.value / 2)
        val newHeight = currentWidth

        // Swap dimensions (width becomes height, height becomes width)
        val finalWidth = newWidth
        val finalHeight = newHeight

        // Add the new format to the list
        defaultPaperFormats.add(
            PaperFormat(
                name = "A$i",
                width = finalWidth.value.toInt().millimeters(),
                height = finalHeight.value.toInt().millimeters()
            )
        )

        // Update dimensions for the next iteration
        currentWidth = finalWidth
        currentHeight = finalHeight
    }

    return defaultPaperFormats
}
