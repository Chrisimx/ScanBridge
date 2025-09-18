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

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class IPPClient(
    private val printerUrl: HttpUrl,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "IPPClient"
        private val IPP_MEDIA_TYPE = "application/ipp".toMediaType()
    }

    /**
     * Get printer attributes to verify connectivity and capabilities
     */
    suspend fun getPrinterAttributes(): Result<IPPResponse> {
        return try {
            val request = IPPMessage.createGetPrinterAttributesRequest(printerUrl.toString())
            val response = executeIPPRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get printer attributes")
            Result.failure(e)
        }
    }

    /**
     * Send a print job with document data
     */
    suspend fun printDocument(
        documentData: ByteArray,
        documentFormat: String,
        jobName: String,
        copies: Int = 1
    ): Result<IPPResponse> {
        return try {
            val request = IPPMessage.createPrintJobRequest(
                printerUrl.toString(),
                documentData,
                documentFormat,
                jobName,
                copies
            )
            val response = executeIPPRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to print document")
            Result.failure(e)
        }
    }

    private suspend fun executeIPPRequest(ippMessage: ByteArray): IPPResponse {
        val requestBody = ippMessage.toRequestBody(IPP_MEDIA_TYPE)
        
        val request = Request.Builder()
            .url(printerUrl)
            .post(requestBody)
            .addHeader("Content-Type", "application/ipp")
            .addHeader("User-Agent", "ScanBridge IPP Client")
            .build()

        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("HTTP error: ${response.code} ${response.message}")
        }

        val responseData = response.body?.bytes() ?: throw IOException("Empty response body")
        return IPPMessage.parseResponse(responseData)
    }
}