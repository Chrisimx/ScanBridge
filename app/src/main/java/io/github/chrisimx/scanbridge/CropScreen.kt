package io.github.chrisimx.scanbridge

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.uicomponents.CroppableAsyncImage
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import io.github.chrisimx.scanbridge.util.cropWithRect
import io.github.chrisimx.scanbridge.util.getEditedImageName
import io.github.chrisimx.scanbridge.util.saveAsJPEG
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.koin.compose.koinInject
import timber.log.Timber

suspend fun finishCrop(cropRect: Rect, file: String): File? = withContext(Dispatchers.IO) {
    val sourceBitmap = BitmapFactory.decodeFile(file)
    if (sourceBitmap == null) {
        Timber.e("Could not decode source bitmap for cropping")
        return@withContext null
    }

    val croppedBitmap = sourceBitmap.cropWithRect(cropRect)
    sourceBitmap.recycle()

    val file = File(file)
    val croppedFile = File(file.parent, file.getEditedImageName())
    croppedBitmap.saveAsJPEG(croppedFile)
    croppedBitmap.recycle()

    return@withContext croppedFile
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTelephotoApi::class)
@Composable
fun CropScreen(scanId: Uuid, returnRoute: BaseRoute, navController: NavController) {
    val context = LocalContext.current
    val db: ScanBridgeDb = koinInject()
    val scannedPageDao = db.scannedPageDao()

    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 5f))
    val coroutineScope = rememberCoroutineScope()
    var currentRect by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }

    var processing: Boolean by remember { mutableStateOf(false) }

    val currentPageMetadata: ScannedPage? by scannedPageDao.getByScanIdFlow(scanId).collectAsState(null)

    val save: () -> Unit = {
        coroutineScope.launch(Dispatchers.Main) {
            val pageMetadata = currentPageMetadata
            if (processing || pageMetadata == null) return@launch

            processing = true
            try {
                val croppedFile = finishCrop(currentRect, pageMetadata.filePath) ?: return@launch
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(Path(pageMetadata.filePath))
                }
                db.scannedPageDao().update(
                    pageMetadata.copy(
                        filePath = croppedFile.absolutePath
                    )
                )
                navController.clearAndNavigateTo(returnRoute)
            } finally {
                processing = false
            }
        }
    }

    BackHandler {
        navController.clearAndNavigateTo(returnRoute)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = save,
                modifier = Modifier.testTag("crop_finish"),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_crop_24),
                        contentDescription = stringResource(R.string.crop)
                    )
                },
                text = { Text(stringResource(R.string.crop_button)) }
            )
        },
        floatingActionButtonPosition = FabPosition.Center

    ) { innerPadding ->
        if (processing) {
            LoadingDialog(text = R.string.cropping)
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
                imageModel = currentPageMetadata?.filePath,
                contentDescription = stringResource(R.string.desc_scanned_page),
                additionalTouchAreaAround = 100.dp,
                handleTouchRadius = 60.dp,
                cropRectChanged = { currentRect = it },
                onPan = {
                    coroutineScope.launch {
                        zoomableState.panBy(it, snap())
                    }
                }
            )
        }
    }
}
