package io.github.chrisimx.scanbridge

import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.cropfeature.CropScreenViewModel
import io.github.chrisimx.scanbridge.cropfeature.CropState
import io.github.chrisimx.scanbridge.platformhelper.PlatformBackHandler
import io.github.chrisimx.scanbridge.uicomponents.CroppableAsyncImage
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.crop
import scanbridge.composeui.generated.resources.crop_button
import scanbridge.composeui.generated.resources.cropping
import scanbridge.composeui.generated.resources.desc_scanned_page
import scanbridge.composeui.generated.resources.outline_crop_24


@OptIn(ExperimentalUuidApi::class)
@Composable
fun CropScreen(
    scanId: Uuid,
    returnRoute: BaseRoute,
    navController: NavController,
    viewModel: CropScreenViewModel = koinInject {
        parametersOf(scanId)
    }
) {
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 5f))
    val coroutineScope = rememberCoroutineScope()

    val croppedImageFilePath by viewModel.croppedImageFilePath.collectAsState()
    val cropState by viewModel.cropState.collectAsState()

    PlatformBackHandler {
        navController.clearAndNavigateTo(returnRoute)
    }

    LaunchedEffect(cropState) {
        if (cropState is CropState.Finished) {
            navController.clearAndNavigateTo(returnRoute)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.finishCrop()
                },
                modifier = Modifier.testTag("crop_finish"),
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.outline_crop_24),
                        contentDescription = stringResource(Res.string.crop)
                    )
                },
                text = { Text(stringResource(Res.string.crop_button)) }
            )
        },
        floatingActionButtonPosition = FabPosition.Center

    ) { innerPadding ->
        if (cropState == CropState.Processing) {
            LoadingDialog(text = Res.string.cropping)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomableState,
                    gestures = EnabledZoomGestures.ZoomOnly
                ),
            contentAlignment = Alignment.CenterHorizontally.plus(Alignment.CenterVertically)
        ) {
            CroppableAsyncImage(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = 40.dp, end = 40.dp, bottom = 100.dp, top = 15.dp),
                imageModel = croppedImageFilePath,
                contentDescription = stringResource(Res.string.desc_scanned_page),
                additionalTouchAreaAround = 100.dp,
                handleTouchRadius = 60.dp,
                cropRectChanged = { composeRect ->
                    viewModel.setCropRect(composeRect.toScanBridgeRect())
                },
                onPan = {
                    coroutineScope.launch {
                        zoomableState.panBy(it, snap())
                    }
                }
            )
        }
    }
}

fun Rect.toScanBridgeRect() = io.github.chrisimx.scanbridge.model.Rect(
    left = left,
    top = top,
    right = right,
    bottom = bottom
)
