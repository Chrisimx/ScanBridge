/*
 *     Copyright (C) 2024 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with eSCLKt.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.chrisimx.scanbridge

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.chrisimx.esclkt.BinaryRendering
import io.github.chrisimx.esclkt.CcdChannel
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.ContentType
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.esclkt.ScanRegions
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

fun JobState?.toJobStateString(context: Context): String {
    return when (this) {
        JobState.Canceled -> context.getString(R.string.job_canceled)
        JobState.Aborted -> context.getString(R.string.job_aborted_because_error)
        JobState.Completed -> context.getString(R.string.job_completed_successfully)
        JobState.Pending -> context.getString(R.string.job_still_pending)
        JobState.Processing -> context.getString(R.string.job_pages_still_processed)
        null -> context.getString(R.string.job_state_cannot_be_retrieved)
    }
}
fun Double.mmToThreeHundredthsOfInch(): UInt {
    return (this / (25.4) / (1.0/300.0)).roundToInt().toUInt()
}
fun Double.threeHundredthsOfInchToMM(): Double {
    return (this * (1.0/300.0) * (25.4))
}
fun UInt.mmToThreeHundredthsOfInch(): UInt {
    return (this.toDouble() / (25.4) / (1.0/300.0)).roundToInt().toUInt()
}
fun UInt.threeHundredthsOfInchToMM(): Double {
    return (this.toDouble() * (1.0/300.0) * (25.4))
}
fun BigDecimal.mmToThreeHundredthsOfInch(): UInt {
    return this.divide(BigDecimal("25.4")).multiply(BigDecimal(300)).toInt().toUInt()
}
fun BigDecimal.threeHundredthsOfInchToMM(): BigDecimal {
    return this.divide(BigDecimal(300)).multiply(BigDecimal("25.4"))
}

data class ScanRegionState(
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
}

data class ScanSettingsState(
    private var versionState: MutableState<String>,
    private var intentState: MutableState<ScanIntentData?> = mutableStateOf(null),
    private var scanRegionsState: MutableState<ScanRegionState?> = mutableStateOf(null),
    private var documentFormatExtState: MutableState<String?> = mutableStateOf(null),
    private var contentTypeState: MutableState<ContentType?> = mutableStateOf(null),
    private var inputSourceState: MutableState<InputSource?> = mutableStateOf(null),
    /** Specified in DPI **/
    private var xResolutionState: MutableState<UInt?> = mutableStateOf(null),
    /** Specified in DPI **/
    private var yResolutionState: MutableState<UInt?> = mutableStateOf(null),
    private var colorModeState: MutableState<ColorMode?> = mutableStateOf(null),
    private var colorSpaceState: MutableState<String?> = mutableStateOf(null),
    private var mediaTypeState: MutableState<String?> = mutableStateOf(null),
    private var ccdChannelState: MutableState<CcdChannel?> = mutableStateOf(null),
    private var binaryRenderingState: MutableState<BinaryRendering?> = mutableStateOf(null),
    private var duplexState: MutableState<Boolean?> = mutableStateOf(null),
    private var numberOfPagesState: MutableState<UInt?> = mutableStateOf(null),
    private var brightnessState: MutableState<UInt?> = mutableStateOf(null),
    private var compressionFactorState: MutableState<UInt?> = mutableStateOf(null),
    private var contrastState: MutableState<UInt?> = mutableStateOf(null),
    private var gammaState: MutableState<UInt?> = mutableStateOf(null),
    private var highlightState: MutableState<UInt?> = mutableStateOf(null),
    private var noiseRemovalState: MutableState<UInt?> = mutableStateOf(null),
    private var shadowState: MutableState<UInt?> = mutableStateOf(null),
    private var sharpenState: MutableState<UInt?> = mutableStateOf(null),
    private var thresholdState: MutableState<UInt?> = mutableStateOf(null),
    /** As per spec:  "opaque information relayed by the client." **/
    private var contextIDState: MutableState<String?> = mutableStateOf(null),
    // private var scanDestinationsState: HTTPDestination?, omitted as no known scanner supports this
    private var blankPageDetectionState: MutableState<Boolean?> = mutableStateOf(null),
    private var feedDirectionState: MutableState<FeedDirection?> = mutableStateOf(null),
    private var blankPageDetectionAndRemovalState: MutableState<Boolean?> = mutableStateOf(null)
) {
    var version by versionState
    var intent by intentState
    var scanRegions by scanRegionsState
    var documentFormatExt by documentFormatExtState
    var contentType by contentTypeState
    var inputSource by inputSourceState
    var xResolution by xResolutionState
    var yResolution by yResolutionState
    var colorMode by colorModeState
    var colorSpace by colorSpaceState
    var mediaType by mediaTypeState
    var ccdChannel by ccdChannelState
    var binaryRendering by binaryRenderingState
    var duplex by duplexState
    var numberOfPages by numberOfPagesState
    var brightness by brightnessState
    var compressionFactor by compressionFactorState
    var contrast by contrastState
    var gamma by gammaState
    var highlight by highlightState
    var noiseRemoval by noiseRemovalState
    var shadow by shadowState
    var sharpen by sharpenState
    var threshold by thresholdState
    var contextID by contextIDState
    var blankPageDetection by blankPageDetectionState
    var feedDirection by feedDirectionState
    var blankPageDetectionAndRemoval by blankPageDetectionAndRemovalState
}