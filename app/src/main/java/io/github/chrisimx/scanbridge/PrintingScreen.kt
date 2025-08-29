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

package io.github.chrisimx.scanbridge

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.ipp.PrinterService
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingScreen(
    printerName: String,
    printerURL: HttpUrl,
    navController: NavController,
    timeout: UInt,
    debug: Boolean,
    certValidationDisabled: Boolean,
    sessionID: String,
    application: Application
) {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }
    var printMessage by remember { mutableStateOf<String?>(null) }
    var copies by remember { mutableStateOf(1) }
    
    val scope = rememberCoroutineScope()
    
    // Create printer service
    val printerService = remember {
        PrinterService(printerURL, timeout, debug, certValidationDisabled)
    }

    // File picker launcher for documents (PDF, images)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileNameFromUri(context, it)
            printMessage = null // Clear any previous messages
            Timber.d("Selected file: $selectedFileName at $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(printerName) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.printers),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Name: $printerName",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "URL: ${printerURL.toString()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.print_document),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (selectedFileName != null) {
                        Text(
                            text = "Selected: $selectedFileName",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.select_file_to_print))
                    }
                    
                    if (selectedFileUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show print status message if any
                        printMessage?.let { message ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (message.contains("success", ignoreCase = true)) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    isPrinting = true
                                    printMessage = null
                                    
                                    try {
                                        // Test connection first
                                        val connectionResult = printerService.testConnection()
                                        if (connectionResult.isFailure) {
                                            printMessage = "Failed to connect to printer: ${connectionResult.exceptionOrNull()?.message}"
                                            return@launch
                                        }
                                        
                                        val printerInfo = connectionResult.getOrThrow()
                                        if (!printerInfo.isOnline) {
                                            printMessage = "Printer is not ready. State: ${printerInfo.state}"
                                            return@launch
                                        }
                                        
                                        // Print the document
                                        val printResult = printerService.printDocument(
                                            context = context,
                                            documentUri = selectedFileUri!!,
                                            jobName = selectedFileName ?: "ScanBridge Print",
                                            copies = copies
                                        )
                                        
                                        if (printResult.isSuccess) {
                                            val result = printResult.getOrThrow()
                                            printMessage = if (result.isSuccess) {
                                                "Print job submitted successfully! Job ID: ${result.jobId}"
                                            } else {
                                                result.message
                                            }
                                        } else {
                                            printMessage = "Print failed: ${printResult.exceptionOrNull()?.message}"
                                        }
                                    } catch (e: Exception) {
                                        printMessage = "Print error: ${e.message}"
                                        Timber.e(e, "Print operation failed")
                                    } finally {
                                        isPrinting = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPrinting
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Printing...")
                            } else {
                                Text(stringResource(R.string.print))
                            }
                        }
                    }
                }
            }
            
            // Print Settings Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.print_settings),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Basic print settings with interactive copies control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.copies))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (copies > 1) copies-- },
                                enabled = copies > 1
                            ) {
                                Text("-", style = MaterialTheme.typography.headlineSmall)
                            }
                            Text(
                                text = copies.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { if (copies < 10) copies++ },
                                enabled = copies < 10
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.paper_size))
                        Text(stringResource(R.string.a4)) // TODO: Make this selectable
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.color_mode))
                        Text(stringResource(R.string.auto)) // TODO: Make this selectable
                    }
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) it.getString(nameIndex) else "Unknown file"
        } else "Unknown file"
    } ?: "Unknown file"
}