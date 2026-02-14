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

package io.github.chrisimx.scanbridge.data.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentEnumOrRaw
import io.github.chrisimx.scanbridge.data.model.MutableScanRegionState

class ScanSettingsComposableViewModel(
    private val _scanSettingsComposableData: ScanSettingsComposableData,
    private val onSettingsChanged: (() -> Unit)? = null
) : ViewModel() {

    val scanSettingsComposableData: ImmutableScanSettingsComposableData
        get() = _scanSettingsComposableData.toImmutable()

    fun getMutableScanSettingsComposableData(): ScanSettingsComposableData = _scanSettingsComposableData

    fun setDuplex(duplex: Boolean) {
        _scanSettingsComposableData.scanSettingsState.duplex = duplex
        onSettingsChanged?.invoke()
    }

    fun setInputSourceOptions(inputSource: InputSource) {
        val scanSettingsState = _scanSettingsComposableData.scanSettingsState
        scanSettingsState.inputSource = inputSource
        revalidateSettings()
        onSettingsChanged?.invoke()
    }

    fun revalidateSettings() {
        val scanSettingsState = _scanSettingsComposableData.scanSettingsState
        if (scanSettingsState.xResolution != null && scanSettingsState.yResolution != null) {
            val isResolutionSupported = _scanSettingsComposableData.supportedScanResolutions.discreteResolutions.contains(
                DiscreteResolution(scanSettingsState.xResolution!!, scanSettingsState.yResolution!!)
            )
            if (!isResolutionSupported) {
                val highestScanResolution = _scanSettingsComposableData.supportedScanResolutions.discreteResolutions.maxBy { it.xResolution * it.yResolution }
                setResolution(highestScanResolution.xResolution, highestScanResolution.yResolution)
            }
        }

        val intentSupported = scanSettingsState.intent?.let { _scanSettingsComposableData.intentOptions.contains(it) }
        if (intentSupported == false) {
            setIntent(null)
        }
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        _scanSettingsComposableData.scanSettingsState.xResolution = xResolution
        _scanSettingsComposableData.scanSettingsState.yResolution = yResolution
        onSettingsChanged?.invoke()
    }

    fun setIntent(intent: ScanIntentEnumOrRaw?) {
        _scanSettingsComposableData.scanSettingsState.intent = intent
        onSettingsChanged?.invoke()
    }

    fun setCustomMenuEnabled(enabled: Boolean) {
        _scanSettingsComposableData.customMenuEnabled = enabled
    }

    fun setWidthTextFieldContent(width: String) {
        _scanSettingsComposableData.widthTextFieldString = width
    }

    fun setHeightTextFieldContent(width: String) {
        _scanSettingsComposableData.heightTextFieldString = width
    }

    fun setRegionDimension(width: String, height: String) {
        if (_scanSettingsComposableData.scanSettingsState.scanRegions == null) {
            _scanSettingsComposableData.scanSettingsState.scanRegions = MutableScanRegionState(
                widthState = mutableStateOf(width),
                heightState = mutableStateOf(height),
                xOffsetState = mutableStateOf("0"),
                yOffsetState = mutableStateOf("0")
            )
            onSettingsChanged?.invoke()
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.width = width.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.height = height.toString()
        onSettingsChanged?.invoke()
    }

    fun setOffset(xOffset: String, yOffset: String) {
        if (_scanSettingsComposableData.scanSettingsState.scanRegions == null) {
            _scanSettingsComposableData.scanSettingsState.scanRegions = MutableScanRegionState(
                widthState = mutableStateOf("0"),
                heightState = mutableStateOf("0"),
                xOffsetState = mutableStateOf(xOffset),
                yOffsetState = mutableStateOf(yOffset)
            )
            onSettingsChanged?.invoke()
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.xOffset = xOffset.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.yOffset = yOffset.toString()
        onSettingsChanged?.invoke()
    }
}
