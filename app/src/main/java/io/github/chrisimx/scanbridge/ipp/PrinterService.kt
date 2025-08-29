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

package io.github.chrisimx.scanbridge.ipp

import android.content.Context
import android.net.Uri
import io.github.chrisimx.scanbridge.logs.DebugInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Service for managing IPP printing operations
 */
class PrinterService(
    private val printerUrl: HttpUrl,
    private val timeout: UInt,
    private val debug: Boolean,
    private val certValidationDisabled: Boolean
) {
    companion object {
        private const val TAG = "PrinterService"
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            if (debug) {
                addInterceptor(DebugInterceptor())
            }
            connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            
            if (certValidationDisabled) {
                try {
                    val (socketFactory, trustManager) = getTrustAllTM()
                    sslSocketFactory(socketFactory, trustManager)
                    hostnameVerifier { _, _ -> true }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed to disable certificate validation")
                }
            }
        }.build()
    }

    private val ippClient: IPPClient by lazy {
        IPPClient(printerUrl, httpClient)
    }

    /**
     * Test printer connectivity and get basic attributes
     */
    suspend fun testConnection(): Result<PrinterInfo> = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Testing connection to printer: $printerUrl")
            
            val result = ippClient.getPrinterAttributes()
            
            if (result.isSuccess) {
                val response = result.getOrThrow()
                if (response.isSuccessful) {
                    val printerName = response.attributes["printer-name"] as? String ?: "Unknown Printer"
                    val printerState = response.attributes["printer-state"] as? Int ?: 3 // idle
                    val documentFormats = response.attributes["document-format-supported"] as? String ?: "application/pdf"
                    
                    val info = PrinterInfo(
                        name = printerName,
                        state = printerState,
                        isOnline = printerState == 3, // 3 = idle, ready to print
                        supportedFormats = listOf(documentFormats)
                    )
                    
                    Timber.tag(TAG).d("Printer connection successful: $info")
                    Result.success(info)
                } else {
                    val error = "Printer returned error: ${response.statusMessage}"
                    Timber.tag(TAG).e(error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = result.exceptionOrNull() ?: Exception("Unknown error")
                Timber.tag(TAG).e(error, "Failed to connect to printer")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception during printer connection test")
            Result.failure(e)
        }
    }

    /**
     * Print a document from URI
     */
    suspend fun printDocument(
        context: Context,
        documentUri: Uri,
        jobName: String = "ScanBridge Print Job",
        copies: Int = 1
    ): Result<PrintJobResult> = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Starting print job: $jobName, copies: $copies")
            
            // Process the document
            val documentResult = DocumentProcessor.processDocument(context, documentUri)
            if (documentResult.isFailure) {
                return@withContext Result.failure(documentResult.exceptionOrNull() ?: Exception("Failed to process document"))
            }
            
            val document = documentResult.getOrThrow()
            
            // Send print job
            val printResult = ippClient.printDocument(
                documentData = document.data,
                documentFormat = document.mimeType,
                jobName = jobName,
                copies = copies
            )
            
            if (printResult.isSuccess) {
                val response = printResult.getOrThrow()
                if (response.isSuccessful) {
                    val jobId = response.attributes["job-id"] as? Int ?: 0
                    val jobState = response.attributes["job-state"] as? Int ?: 3 // pending
                    
                    val result = PrintJobResult(
                        jobId = jobId,
                        jobState = jobState,
                        message = "Print job submitted successfully",
                        isSuccess = true
                    )
                    
                    Timber.tag(TAG).d("Print job successful: $result")
                    Result.success(result)
                } else {
                    val error = "Print job failed: ${response.statusMessage}"
                    Timber.tag(TAG).e(error)
                    val result = PrintJobResult(
                        jobId = 0,
                        jobState = 9, // aborted
                        message = error,
                        isSuccess = false
                    )
                    Result.success(result) // Return error result but not exception
                }
            } else {
                val error = printResult.exceptionOrNull() ?: Exception("Unknown print error")
                Timber.tag(TAG).e(error, "Print job failed")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception during print operation")
            Result.failure(e)
        }
    }
}

/**
 * Information about a printer
 */
data class PrinterInfo(
    val name: String,
    val state: Int,
    val isOnline: Boolean,
    val supportedFormats: List<String>
)

/**
 * Result of a print job operation
 */
data class PrintJobResult(
    val jobId: Int,
    val jobState: Int,
    val message: String,
    val isSuccess: Boolean
)

// Trust manager for certificate validation bypass (same as used in scanner code)
private fun getTrustAllTM(): Pair<javax.net.ssl.SSLSocketFactory, javax.net.ssl.X509TrustManager> {
    val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
        object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    )
    
    val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    
    return sslContext.socketFactory to trustAllCerts[0] as javax.net.ssl.X509TrustManager
}