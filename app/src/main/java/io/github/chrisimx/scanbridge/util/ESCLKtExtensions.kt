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
import android.icu.number.NumberFormatter
import android.icu.text.DecimalFormat
import androidx.compose.runtime.mutableStateOf
import io.github.chrisimx.esclkt.ColorModeEnumOrRaw
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.EnumOrRaw
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.esclkt.scanRegion
import io.github.chrisimx.scanbridge.R

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


fun InputSource.toReadableString(context: Context): String = when (this) {
    InputSource.Platen -> context.getString(R.string.platen)
    InputSource.Feeder -> context.getString(R.string.adf)
    InputSource.Camera -> context.getString(R.string.camera)
}

fun ScannerCapabilities.getMaxResolution(inputSource: InputSource): DiscreteResolution {
    val inputCaps = this.getInputSourceCaps(inputSource)
    val maxResolution = inputCaps
        .settingProfiles.first()
        .supportedResolutions.discreteResolutions.maxBy { it.xResolution * it.yResolution }

    return maxResolution
}

fun ScannerCapabilities.getBestColorMode(inputSource: InputSource): ColorModeEnumOrRaw? {
    val inputCaps = this.getInputSourceCaps(inputSource)
    val chosenColorMode = inputCaps.settingProfiles.elementAtOrNull(0)?.colorModes?.maxByOrNull {
        when (it) {
            is EnumOrRaw.Known -> it.value.ordinal
            is EnumOrRaw.Unknown -> 0
        }
    }
    return chosenColorMode
}

fun ScannerCapabilities.calculateDefaultESCLScanSettingsState(): ScanSettings {
    val inputSource = this.getInputSourceOptions().firstOrNull() ?: InputSource.Platen

    val maxResolution = getMaxResolution(inputSource)

    val inputSourceCaps = this.getInputSourceCaps(inputSource, false)

    val maxScanRegion = scanRegion(inputSourceCaps) {
        maxHeight()
        maxWidth()
    }

    val bestColorMode = getBestColorMode(inputSource)

    return ScanSettings(
        version = this.interfaceVersion,
        inputSource = inputSource,
        scanRegions = maxScanRegion,
        xResolution = maxResolution.xResolution,
        yResolution = maxResolution.yResolution,
        colorMode = bestColorMode,
        documentFormatExt = "image/jpeg"
    )
}
