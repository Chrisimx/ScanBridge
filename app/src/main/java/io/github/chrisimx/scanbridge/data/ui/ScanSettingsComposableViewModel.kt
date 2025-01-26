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
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.scanbridge.data.model.MutableScanRegionState

class ScanSettingsComposableViewModel(private val _scanSettingsComposableData: ScanSettingsComposableData) : ViewModel() {

    val scanSettingsComposableData: ImmutableScanSettingsComposableData
        get() = _scanSettingsComposableData.toImmutable()

    fun setDuplex(duplex: Boolean) {
        _scanSettingsComposableData.scanSettingsState.duplex = duplex
    }

    fun setInputSourceOptions(inputSource: InputSource) {
        _scanSettingsComposableData.scanSettingsState.inputSource = inputSource
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        _scanSettingsComposableData.scanSettingsState.xResolution = xResolution
        _scanSettingsComposableData.scanSettingsState.yResolution = yResolution
    }

    fun setIntent(intent: ScanIntentData?) {
        _scanSettingsComposableData.scanSettingsState.intent = intent
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
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.width = width.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.height = height.toString()
    }

    fun setOffset(xOffset: String, yOffset: String) {
        if (_scanSettingsComposableData.scanSettingsState.scanRegions == null) {
            _scanSettingsComposableData.scanSettingsState.scanRegions = MutableScanRegionState(
                widthState = mutableStateOf("0"),
                heightState = mutableStateOf("0"),
                xOffsetState = mutableStateOf(xOffset),
                yOffsetState = mutableStateOf(yOffset)
            )
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.xOffset = xOffset.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.yOffset = yOffset.toString()
    }
}
