package io.github.chrisimx.scanbridge.data.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.ScanRegion
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import kotlinx.serialization.Serializable

@Serializable
data class ImmutableScanRegionState(
    // These are to be given in millimeters!
    private val heightState: State<String>,
    private val widthState: State<String>,
    private val xOffsetState: State<String>,
    private val yOffsetState: State<String>
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

@Serializable
data class StatelessImmutableScanRegion(
    // These are to be given in millimeters!
    val height: String,
    val width: String,
    val xOffset: String,
    val yOffset: String
) {

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

    fun toMutable(): MutableScanRegionState = MutableScanRegionState(
        mutableStateOf(height),
        mutableStateOf(width),
        mutableStateOf(xOffset),
        mutableStateOf(yOffset)
    )
}

@Serializable
data class MutableScanRegionState(
    // These are to be given in millimeters!
    private val heightState: MutableState<String>,
    private val widthState: MutableState<String>,
    private val xOffsetState: MutableState<String> = mutableStateOf("0"),
    private val yOffsetState: MutableState<String> = mutableStateOf("0")
) {
    var height by heightState
    var width by widthState
    var xOffset by xOffsetState
    var yOffset by yOffsetState

    fun toImmutable(): ImmutableScanRegionState = ImmutableScanRegionState(heightState, widthState, xOffsetState, yOffsetState)
    fun toStateless(): StatelessImmutableScanRegion =
        StatelessImmutableScanRegion(heightState.value, widthState.value, xOffsetState.value, yOffsetState.value)
    fun toESCLScanRegion(selectedInputSourceCaps: InputSourceCaps): ScanRegion = toImmutable().toESCLScanRegion(selectedInputSourceCaps)
}
