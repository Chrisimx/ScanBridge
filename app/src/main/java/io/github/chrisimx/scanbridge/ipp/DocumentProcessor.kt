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
import timber.log.Timber
import java.io.IOException

/**
 * Helper class to handle document processing for printing
 */
object DocumentProcessor {
    private const val TAG = "DocumentProcessor"

    /**
     * Read document data from URI and determine MIME type
     */
    fun processDocument(context: Context, uri: Uri): Result<DocumentData> {
        return try {
            val contentResolver = context.contentResolver
            
            // Get MIME type
            val mimeType = contentResolver.getType(uri) ?: run {
                // Fallback to detecting from file extension
                val fileName = getFileNameFromUri(context, uri)
                when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    fileName.endsWith(".jpg", ignoreCase = true) || 
                    fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "application/octet-stream"
                }
            }
            
            // Read document data
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IOException("Cannot open input stream for URI"))
            
            val documentData = inputStream.use { it.readBytes() }
            
            if (documentData.isEmpty()) {
                return Result.failure(IOException("Document is empty"))
            }
            
            val fileName = getFileNameFromUri(context, uri)
            
            Timber.tag(TAG).d("Processed document: $fileName, type: $mimeType, size: ${documentData.size} bytes")
            
            Result.success(DocumentData(documentData, mimeType, fileName))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to process document from URI: $uri")
            Result.failure(e)
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else "Unknown file"
            } else "Unknown file"
        } ?: "Unknown file"
    }
}

/**
 * Data class representing a processed document ready for printing
 */
data class DocumentData(
    val data: ByteArray,
    val mimeType: String,
    val fileName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentData

        if (!data.contentEquals(other.data)) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}