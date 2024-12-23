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

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ScanningActivityData(
    val scanSettingsVM: ScanSettingsViewModel,
    val capabilities: ScannerCapabilities,
    val scanSettingsMenuOpen: MutableState<Boolean> = mutableStateOf(false),
    val scanJobRunning: MutableState<Boolean> = mutableStateOf(false),
    val stateCurrentScans: SnapshotStateList<String> = mutableStateListOf()
) {
    fun toImmutable() = ImmutableScanningActivityData(
        scanSettingsVM,
        capabilities,
        scanSettingsMenuOpen,
        scanJobRunning,
        stateCurrentScans
    )
}

data class ImmutableScanningActivityData(
    val scanSettingsVM: ScanSettingsViewModel,
    val capabilities: ScannerCapabilities,
    private val scanSettingsMenuOpenState: State<Boolean>,
    private val scanJobRunningState: State<Boolean>,
    val currentScansState: SnapshotStateList<String>,
) {
    val scanSettingsMenuOpen by scanSettingsMenuOpenState
    val scanJobRunning by scanJobRunningState
}

class ScanningViewModel(
    scanSettingsVM: ScanSettingsViewModel,
    capabilities: ScannerCapabilities
) : ViewModel() {
    private val _scanningActivityData =
        ScanningActivityData(scanSettingsVM, capabilities)
    val scanningActivityData: ImmutableScanningActivityData
        get() = _scanningActivityData.toImmutable()

    fun setScanSettingsMenuOpen(value: Boolean) {
        _scanningActivityData.scanSettingsMenuOpen.value = value
    }

    fun setScanJobRunning(value: Boolean) {
        _scanningActivityData.scanJobRunning.value = value
    }

    fun addScan(path: String) {
        _scanningActivityData.stateCurrentScans.add(path)
    }
}


class ScanningActivity : ComponentActivity() {

    lateinit var scannerName: String
    lateinit var scannerAddress: HttpUrl
    lateinit var scannerCapabilities: ScannerCapabilities

    lateinit var esclRequestClient: ESCLRequestClient

    private val TAG: String = this.javaClass.simpleName

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Activity stopped")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Activity started")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "Activity restore instance state")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun doScan(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        scanSettings: ScanSettings,
        viewModel: ScanningViewModel,
    ) {
        thread {
            if (viewModel.scanningActivityData.scanJobRunning) {
                snackBarError("Job still running", scope, snackbarHostState, false)
                return@thread
            }

            viewModel.setScanJobRunning(true)
            val job =
                esclRequestClient.createJob(scanSettings)
            Log.d(TAG, "Sent scan request to scanner. Result: $job")
            if (job !is ESCLRequestClient.ScannerCreateJobResult.Success) {
                viewModel.setScanJobRunning(false)
                snackBarError(job.toString(), scope, snackbarHostState)
                return@thread
            }

            while (true) {
                var nextPage = job.scanJob.retrieveNextPage()
                val status = job.scanJob.getJobStatus()
                val jobStateString = status?.jobState.toJobStateString(this)
                if (nextPage is ESCLRequestClient.ScannerNextPageResult.NoFurtherPages) {
                    viewModel.setScanJobRunning(false)
                    scope.launch {
                        snackbarHostState.showSnackbar("No further pages. $jobStateString")
                    }
                    return@thread
                } else if (nextPage !is ESCLRequestClient.ScannerNextPageResult.Success) {
                    viewModel.setScanJobRunning(false)
                    return@thread
                }
                nextPage.page.use {
                    val uuid = "scan-"+Uuid.random().toString()
                    val filePath = File(filesDir, uuid.toString()).toPath()

                    Log.d(TAG, "File created: $filePath")

                    Files.copy(it.data.body!!.byteStream(), filePath)
                    val imageBitmap = BitmapFactory.decodeFile(filePath.toString())?.asImageBitmap()
                    if (imageBitmap == null) {
                        Log.e(TAG, "Couldn't decode received image")
                        snackBarError("Couldn't decode received image. $jobStateString", scope, snackbarHostState)
                        filePath.toFile().delete()
                        return@thread
                    }
                    viewModel.addScan(filePath.toString())
                }
            }
        }
    }

    fun snackBarError(error: String, scope: CoroutineScope, snackbarHostState: SnackbarHostState, action: Boolean = true) {
        scope.launch {
            val result = snackbarHostState.showSnackbar("Error while retrieving page: $error", if (action) "Copy" else null, true)
            when (result) {
                SnackbarResult. ActionPerformed -> {
                    val systemClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    systemClipboard.setPrimaryClip(ClipData.newPlainText("Error", error))
                }
                SnackbarResult. Dismissed -> { }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity created")

        val intent = getIntent()

        val extraScannerName = intent.getStringExtra("name")
        val extraScannerAddress = intent.getStringExtra("address")?.toHttpUrl()

        enableEdgeToEdge()

        if (extraScannerName == null || extraScannerAddress == null) {
            setContent {
                ErrorDialog(stringResource(
                    R.string.activity_extras_missing,
                    TAG,
                    "scannerName: $scannerName\nscannerAddress: $scannerAddress"
                ), { this.finish() })
            }
            return
        } else {
            this.scannerName = extraScannerName
            this.scannerAddress = extraScannerAddress
        }

        esclRequestClient = ESCLRequestClient(scannerAddress, OkHttpClient.Builder().build())

        setContent {
            ScanBridgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoadingScreen(innerPadding)
                }
            }
        }
        thread {
            val retrievedScannerCapabilities = esclRequestClient.getScannerCapabilities()
            Log.d(TAG, "$retrievedScannerCapabilities")

            if (retrievedScannerCapabilities !is ESCLRequestClient.ScannerCapabilitiesResult.Success) {
                setContent {
                    ErrorDialog(stringResource(
                        R.string.scannercapabilities_retrieve_error,
                        retrievedScannerCapabilities
                    ), { this.finish() })
                }
                return@thread
            }
            this.scannerCapabilities = retrievedScannerCapabilities.scannerCapabilities
            setContent {
                val scanningViewModel = viewModel {
                    val scanSettingsViewModel = ScanSettingsViewModel(
                        MutableESCLScanSettingsState(
                            versionState = mutableStateOf(scannerCapabilities.interfaceVersion),
                            scanRegionsState = mutableStateOf(
                                MutableScanRegionState(
                                    heightState = mutableStateOf("0"),
                                    widthState = mutableStateOf("0"),
                                    xOffsetState = mutableStateOf("0"),
                                    yOffsetState = mutableStateOf("0")
                                )
                            ),
                        ), scannerCapabilities
                    )
                    ScanningViewModel(scanSettingsViewModel, scannerCapabilities)
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                ScanBridgeTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) { data ->
                            Snackbar(data, containerColor = if (data.visuals.message.contains("Error")) MaterialTheme.colorScheme.error else SnackbarDefaults.color)
                        }},
                        topBar = {

                        },
                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    IconButton(onClick = {
                                        scanningViewModel.setScanSettingsMenuOpen(
                                            true
                                        )
                                    }) {
                                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                                    }
                                },
                                floatingActionButton = {
                                    ExtendedFloatingActionButton (onClick = {
                                        doScan(
                                            scope,
                                            snackbarHostState,
                                            scanningViewModel.scanningActivityData.scanSettingsVM.scanSettingsComposableData.scanSettingsState.toESCLKtScanSettings(
                                                scanningViewModel.scanningActivityData.scanSettingsVM.scanSettingsComposableData.selectedInputSourceCapabilities
                                            ),
                                            scanningViewModel
                                        )
                                    },
                                        icon = { Icon(
                                            painterResource(R.drawable.outline_scan_24),
                                            contentDescription = stringResource(
                                                R.string.scan
                                            )
                                        ) },
                                        text = { Text("Scan") }
                                    )
                                }
                            )

                        },
                    ) { innerPadding ->
                        val pagerState = rememberPagerState(pageCount = {
                            scanningViewModel.scanningActivityData.currentScansState.size
                        }
                        )

                        HorizontalPager(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            state = pagerState
                        ) { page ->
                            val zoomState = rememberZoomableState(zoomSpec = ZoomSpec(5f))

                            Column {
                                AsyncImage(
                                    model = scanningViewModel.scanningActivityData.currentScansState[page],
                                    contentDescription = stringResource(R.string.desc_scanned_page),
                                    modifier = Modifier
                                        .zoomable(zoomState)
                                        .padding(vertical = 5.dp),

                                    )
                            }
                        }

                        if (scanningViewModel.scanningActivityData.scanSettingsMenuOpen) {
                            ModalBottomSheet({ scanningViewModel.setScanSettingsMenuOpen(false) }) {
                                ScanSettingsUI(
                                    Modifier,
                                    scanningViewModel.scanningActivityData.scanSettingsVM
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LoadingScreen(innerPadding: PaddingValues) {
    Column(modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(100.dp), strokeWidth = 16.dp)
        Text(modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp),
            text = stringResource(R.string.trying_to_retrieve_scannercapabilities),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            lineHeight = 32.sp,
            fontSize = 24.sp)
    }

}