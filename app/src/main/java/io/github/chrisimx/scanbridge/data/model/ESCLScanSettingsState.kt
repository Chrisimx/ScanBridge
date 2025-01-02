package io.github.chrisimx.scanbridge.data.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.chrisimx.esclkt.BinaryRendering
import io.github.chrisimx.esclkt.CcdChannel
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.ContentType
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.esclkt.ScanRegions
import io.github.chrisimx.esclkt.ScanSettings

data class MutableESCLScanSettingsState(
    private var versionState: MutableState<String>,
    private var intentState: MutableState<ScanIntentData?> = mutableStateOf(null),
    private var scanRegionsState: MutableState<MutableScanRegionState?> = mutableStateOf(null),
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

    fun toImmutable(): ImmutableESCLScanSettingsState {
        return ImmutableESCLScanSettingsState(
            versionState,
            intentState,
            derivedStateOf { scanRegionsState.value?.toImmutable() },
            documentFormatExtState,
            contentTypeState,
            inputSourceState,
            xResolutionState,
            yResolutionState,
            colorModeState,
            colorSpaceState,
            mediaTypeState,
            ccdChannelState,
            binaryRenderingState,
            duplexState,
            numberOfPagesState,
            brightnessState,
            compressionFactorState,
            contrastState,
            gammaState,
            highlightState,
            noiseRemovalState,
            shadowState,
            sharpenState,
            thresholdState,
            contextIDState,
            blankPageDetectionState,
            feedDirectionState,
            blankPageDetectionAndRemovalState
        )
    }
    fun toESCLKtScanSettings(selectedInputSourceCaps: InputSourceCaps): ScanSettings {
        return toImmutable().toESCLKtScanSettings(selectedInputSourceCaps)
    }
}

data class ImmutableESCLScanSettingsState(
    val versionState: State<String>,
    val intentState: State<ScanIntentData?>,
    val scanRegionsState: State<ImmutableScanRegionState?>,
    val documentFormatExtState: State<String?>,
    val contentTypeState: State<ContentType?>,
    val inputSourceState: State<InputSource?>,
    val xResolutionState: State<UInt?>,
    val yResolutionState: State<UInt?>,
    val colorModeState: State<ColorMode?>,
    val colorSpaceState: State<String?>,
    val mediaTypeState: State<String?>,
    val ccdChannelState: State<CcdChannel?>,
    val binaryRenderingState: State<BinaryRendering?>,
    val duplexState: State<Boolean?>,
    val numberOfPagesState: State<UInt?>,
    val brightnessState: State<UInt?>,
    val compressionFactorState: State<UInt?>,
    val contrastState: State<UInt?>,
    val gammaState: State<UInt?>,
    val highlightState: State<UInt?>,
    val noiseRemovalState: State<UInt?>,
    val shadowState: State<UInt?>,
    val sharpenState: State<UInt?>,
    val thresholdState: State<UInt?>,
    val contextIDState: State<String?>,
    val blankPageDetectionState: State<Boolean?>,
    val feedDirectionState: State<FeedDirection?>,
    val blankPageDetectionAndRemovalState: State<Boolean?>
) {
    // Declare properties with only a getter
    val version by versionState
    val intent by intentState
    val scanRegions by scanRegionsState
    val documentFormatExt by documentFormatExtState
    val contentType by contentTypeState
    val inputSource by inputSourceState
    val xResolution by xResolutionState
    val yResolution by yResolutionState
    val colorMode by colorModeState
    val colorSpace by colorSpaceState
    val mediaType by mediaTypeState
    val ccdChannel by ccdChannelState
    val binaryRendering by binaryRenderingState
    val duplex by duplexState
    val numberOfPages by numberOfPagesState
    val brightness by brightnessState
    val compressionFactor by compressionFactorState
    val contrast by contrastState
    val gamma by gammaState
    val highlight by highlightState
    val noiseRemoval by noiseRemovalState
    val shadow by shadowState
    val sharpen by sharpenState
    val threshold by thresholdState
    val contextID by contextIDState
    val blankPageDetection by blankPageDetectionState
    val feedDirection by feedDirectionState
    val blankPageDetectionAndRemoval by blankPageDetectionAndRemovalState

    fun toESCLKtScanSettings(selectedInputSourceCaps: InputSourceCaps): ScanSettings {
        val scanRegionsESCL = if (scanRegions != null) {
            listOf(scanRegions!!.toESCLScanRegion(selectedInputSourceCaps))
        } else {
            emptyList()
        }
        return ScanSettings(
            version = version,
            intent = intent,
            scanRegions = ScanRegions(scanRegionsESCL),
            documentFormatExt = documentFormatExt,
            contentType = contentType,
            inputSource = inputSource,
            xResolution = xResolution,
            yResolution = yResolution,
            colorMode = colorMode,
            colorSpace = colorSpace,
            mediaType = mediaType,
            ccdChannel = ccdChannel,
            binaryRendering = binaryRendering,
            duplex = duplex,
            numberOfPages = numberOfPages,
            brightness = brightness,
            compressionFactor = compressionFactor,
            contrast = contrast,
            gamma = gamma,
            highlight = highlight,
            noiseRemoval = noiseRemoval,
            shadow = shadow,
            sharpen = sharpen,
            threshold = threshold,
            contextID = contextID,
            blankPageDetection = blankPageDetection,
            feedDirection = feedDirection,
            blankPageDetectionAndRemoval = blankPageDetectionAndRemoval
        )
    }
}