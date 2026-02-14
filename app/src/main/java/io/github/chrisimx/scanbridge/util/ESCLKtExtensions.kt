/*
 *     Copyright (C) 2024-2025 Christian Nagel and contributors
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

package io.github.chrisimx.scanbridge.util

import android.content.Context
import android.icu.text.DecimalFormat
import androidx.compose.runtime.mutableStateOf
import io.github.chrisimx.esclkt.EnumOrRaw
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.data.model.MutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.MutableScanRegionState

fun JobState?.toJobStateString(context: Context): String = when (this) {
    JobState.Canceled -> context.getString(R.string.job_canceled)
    JobState.Aborted -> context.getString(R.string.job_aborted_because_error)
    JobState.Completed -> context.getString(R.string.job_completed_successfully)
    JobState.Pending -> context.getString(R.string.job_still_pending)
    JobState.Processing -> context.getString(R.string.job_pages_still_processed)
    null -> context.getString(R.string.job_state_cannot_be_retrieved)
}

fun String.toDoubleLocalized(): Double = DecimalFormat.getInstance().parse(this).toDouble()

fun Double.toStringLocalized(): String = DecimalFormat.getInstance().format(this)

fun ScannerCapabilities.getInputSourceOptions(): List<InputSource> {
    val tmpInputSourceOptions = mutableListOf<InputSource>()
    if (this.platen != null) {
        tmpInputSourceOptions.add(InputSource.Platen)
    }
    if (this.adf != null) {
        tmpInputSourceOptions.add(InputSource.Feeder)
    }
    return tmpInputSourceOptions
}

fun ScannerCapabilities.getInputSourceCaps(inputSource: InputSource, duplex: Boolean = false): InputSourceCaps = when (inputSource) {
    InputSource.Platen -> this.platen!!.inputSourceCaps
    InputSource.Feeder -> if (duplex) this.adf!!.duplexCaps!! else this.adf!!.simplexCaps
    InputSource.Camera -> TODO()
}

fun InputSource.toReadableString(context: Context): String = when (this) {
    InputSource.Platen -> context.getString(R.string.platen)
    InputSource.Feeder -> context.getString(R.string.adf)
    InputSource.Camera -> context.getString(R.string.camera)
}

fun ScannerCapabilities.calculateDefaultESCLScanSettingsState(): MutableESCLScanSettingsState {
    val inputSource = this.getInputSourceOptions().firstOrNull() ?: InputSource.Platen
    val inputCaps = this.getInputSourceCaps(inputSource)
    val maxResolution = inputCaps
        .settingProfiles.first()
        .supportedResolutions.discreteResolutions.maxBy { it.xResolution }
    val maxScanRegion = MutableScanRegionState(
        heightState = mutableStateOf("max"),
        widthState = mutableStateOf("max")
    )

    val chosenColorMode = inputCaps.settingProfiles.elementAtOrNull(0)?.colorModes?.maxByOrNull {
        when (it) {
            is EnumOrRaw.Known -> it.value.ordinal
            is EnumOrRaw.Unknown -> 0
        }
    }

    return MutableESCLScanSettingsState(
        versionState = mutableStateOf(this.interfaceVersion),
        inputSourceState = mutableStateOf(inputSource),
        scanRegionsState = mutableStateOf(maxScanRegion),
        xResolutionState = mutableStateOf(maxResolution.xResolution),
        yResolutionState = mutableStateOf(maxResolution.yResolution),
        colorModeState = mutableStateOf(chosenColorMode),
        documentFormatExtState = mutableStateOf("image/jpeg")
    )
}
