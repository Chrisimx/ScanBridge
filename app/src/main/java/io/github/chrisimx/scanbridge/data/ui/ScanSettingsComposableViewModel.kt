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
import io.github.chrisimx.esclkt.BinaryRendering
import io.github.chrisimx.esclkt.CcdChannel
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.ContentType
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentData
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
        _scanSettingsComposableData.scanSettingsState.inputSource = inputSource
        onSettingsChanged?.invoke()
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        _scanSettingsComposableData.scanSettingsState.xResolution = xResolution
        _scanSettingsComposableData.scanSettingsState.yResolution = yResolution
        onSettingsChanged?.invoke()
    }

    fun setIntent(intent: ScanIntentData?) {
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

    fun setXOffsetTextFieldContent(xOffset: String) {
        _scanSettingsComposableData.xOffsetTextFieldString = xOffset
    }

    fun setYOffsetTextFieldContent(yOffset: String) {
        _scanSettingsComposableData.yOffsetTextFieldString = yOffset
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

    // Content Type
    fun setContentType(contentType: ContentType?) {
        _scanSettingsComposableData.scanSettingsState.contentType = contentType
        onSettingsChanged?.invoke()
    }

    // Color Mode
    fun setColorMode(colorMode: ColorMode?) {
        _scanSettingsComposableData.scanSettingsState.colorMode = colorMode
        onSettingsChanged?.invoke()
    }

    // Color Space
    fun setColorSpace(colorSpace: String?) {
        _scanSettingsComposableData.scanSettingsState.colorSpace = colorSpace
        onSettingsChanged?.invoke()
    }

    // Media Type
    fun setMediaType(mediaType: String?) {
        _scanSettingsComposableData.scanSettingsState.mediaType = mediaType
        onSettingsChanged?.invoke()
    }

    // CCD Channel
    fun setCcdChannel(ccdChannel: CcdChannel?) {
        _scanSettingsComposableData.scanSettingsState.ccdChannel = ccdChannel
        onSettingsChanged?.invoke()
    }

    // Binary Rendering
    fun setBinaryRendering(binaryRendering: BinaryRendering?) {
        _scanSettingsComposableData.scanSettingsState.binaryRendering = binaryRendering
        onSettingsChanged?.invoke()
    }

    // Threshold
    fun setThreshold(threshold: UInt?) {
        _scanSettingsComposableData.scanSettingsState.threshold = threshold
        onSettingsChanged?.invoke()
    }

    // Number of Pages
    fun setNumberOfPages(numberOfPages: UInt?) {
        _scanSettingsComposableData.scanSettingsState.numberOfPages = numberOfPages
        onSettingsChanged?.invoke()
    }

    // Blank Page Detection
    fun setBlankPageDetection(blankPageDetection: Boolean?) {
        _scanSettingsComposableData.scanSettingsState.blankPageDetection = blankPageDetection
        onSettingsChanged?.invoke()
    }

    // Blank Page Detection and Removal
    fun setBlankPageDetectionAndRemoval(blankPageDetectionAndRemoval: Boolean?) {
        _scanSettingsComposableData.scanSettingsState.blankPageDetectionAndRemoval = blankPageDetectionAndRemoval
        onSettingsChanged?.invoke()
    }

    // Feed Direction
    fun setFeedDirection(feedDirection: FeedDirection?) {
        _scanSettingsComposableData.scanSettingsState.feedDirection = feedDirection
        onSettingsChanged?.invoke()
    }

    // Effects - Brightness
    fun setBrightness(brightness: UInt?) {
        _scanSettingsComposableData.scanSettingsState.brightness = brightness
        onSettingsChanged?.invoke()
    }

    // Effects - Compression Factor
    fun setCompressionFactor(compressionFactor: UInt?) {
        _scanSettingsComposableData.scanSettingsState.compressionFactor = compressionFactor
        onSettingsChanged?.invoke()
    }

    // Effects - Contrast
    fun setContrast(contrast: UInt?) {
        _scanSettingsComposableData.scanSettingsState.contrast = contrast
        onSettingsChanged?.invoke()
    }

    // Effects - Gamma
    fun setGamma(gamma: UInt?) {
        _scanSettingsComposableData.scanSettingsState.gamma = gamma
        onSettingsChanged?.invoke()
    }

    // Effects - Highlight
    fun setHighlight(highlight: UInt?) {
        _scanSettingsComposableData.scanSettingsState.highlight = highlight
        onSettingsChanged?.invoke()
    }

    // Effects - Noise Removal
    fun setNoiseRemoval(noiseRemoval: UInt?) {
        _scanSettingsComposableData.scanSettingsState.noiseRemoval = noiseRemoval
        onSettingsChanged?.invoke()
    }

    // Effects - Shadow
    fun setShadow(shadow: UInt?) {
        _scanSettingsComposableData.scanSettingsState.shadow = shadow
        onSettingsChanged?.invoke()
    }

    // Effects - Sharpen
    fun setSharpen(sharpen: UInt?) {
        _scanSettingsComposableData.scanSettingsState.sharpen = sharpen
        onSettingsChanged?.invoke()
    }
}
