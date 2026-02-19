package io.github.chrisimx.scanbridge

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.chrisimx.scanbridge.data.model.Session
import io.github.chrisimx.scanbridge.stores.SessionsStore
import io.github.chrisimx.scanbridge.uicomponents.CroppableAsyncImage
import io.github.chrisimx.scanbridge.uicomponents.dialog.LoadingDialog
import io.github.chrisimx.scanbridge.util.clearAndNavigateTo
import io.github.chrisimx.scanbridge.util.cropWithRect
import io.github.chrisimx.scanbridge.util.getEditedImageName
import io.github.chrisimx.scanbridge.util.saveAsJPEG
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
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

private fun updateSessionFile(
    originalSession: Session,
    pageIdx: Int,
    croppedFile: File,
    originalImageFile: String,
    context: Context,
    sessionID: String
) {
    val scannedPages = originalSession.scannedPages.toMutableList()
    val tempPages = originalSession.tmpFiles.toMutableList()

    val oldMetaData = scannedPages.removeAt(pageIdx)
    scannedPages.add(
        pageIdx,
        oldMetaData.copy(filePath = croppedFile.absolutePath)
    )

    tempPages.add(originalImageFile)

    val editedSession = originalSession.copy(scannedPages = scannedPages, tmpFiles = tempPages)
    SessionsStore.saveSession(editedSession, context, sessionID)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTelephotoApi::class)
@Composable
fun CropScreen(sessionID: String, pageIdx: Int, returnRoute: BaseRoute, navController: NavController) {
    val context = LocalContext.current

    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 5f))
    val coroutineScope = rememberCoroutineScope()
    var currentRect by remember { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }

    var processing: Boolean by remember { mutableStateOf(false) }

    val originalSessionResult: Result<Session?> = remember {
        SessionsStore.loadSession(context, sessionID)
    }

    if (originalSessionResult.getOrNull() == null) {
        Timber.e("Could not decode/get session with $sessionID but CropScreen was opened")
        navController.clearAndNavigateTo(StartUpScreenRoute)
        return
    }

    val originalSession = originalSessionResult.getOrThrow()!!

    val originalImageFile = remember { originalSession.scannedPages[pageIdx].filePath }

    val save: () -> Unit = {
        coroutineScope.launch(Dispatchers.Main) {
            if (processing) return@launch

            processing = true
            try {
                val croppedFile = finishCrop(currentRect, originalImageFile) ?: return@launch

                updateSessionFile(originalSession, pageIdx, croppedFile, originalImageFile, context, sessionID)
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
                    .padding(40.dp),
                imageModel = originalImageFile,
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
