package io.github.chrisimx.scanbridge.data.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.scanbridge.util.toDoubleLocalized

data class ImmutableScanRegionState(
    // These are to be given in millimeters!
    private val heightState: State<String>,
    private val widthState: State<String>,
    private val xOffsetState: State<String>,
    private val yOffsetState: State<String>,
) {
    val height by heightState
    val width by widthState
    val xOffset by xOffsetState
    val yOffset by yOffsetState

    fun toESCLScanRegion(selectedInputSourceCaps: InputSourceCaps): ScanRegion {
        val height: LengthUnit = when (height) {
            "max" -> selectedInputSourceCaps.maxHeight
            else -> height.toDoubleLocalized().millimeters()
        }
        val width: LengthUnit = when (width) {
            "max" -> selectedInputSourceCaps.maxWidth
            else -> width.toDoubleLocalized().millimeters()
        }

        return ScanRegion(
            height.toThreeHundredthsOfInch(),
            width.toThreeHundredthsOfInch(),
            xOffset.toDoubleLocalized().millimeters().toThreeHundredthsOfInch(),
            yOffset.toDoubleLocalized().millimeters().toThreeHundredthsOfInch()
        )
    }
}

data class MutableScanRegionState(
    // These are to be given in millimeters!
    private val heightState: MutableState<String>,
    private val widthState: MutableState<String>,
    private val xOffsetState: MutableState<String>,
    private val yOffsetState: MutableState<String>,
) {
    var height by heightState
    var width by widthState
    var xOffset by xOffsetState
    var yOffset by yOffsetState

    fun toImmutable(): ImmutableScanRegionState {
        return ImmutableScanRegionState(heightState, widthState, xOffsetState, yOffsetState)
    }
    fun toESCLScanRegion(selectedInputSourceCaps: InputSourceCaps): ScanRegion {
        return toImmutable().toESCLScanRegion(selectedInputSourceCaps)
    }
}