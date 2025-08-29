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

import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.random.Random

/**
 * IPP Message parser and builder for the Internet Printing Protocol
 */
object IPPMessage {
    private const val TAG = "IPPMessage"
    
    // IPP version
    private const val IPP_VERSION_MAJOR: Byte = 1
    private const val IPP_VERSION_MINOR: Byte = 1
    
    // Attribute tags
    private const val TAG_OPERATION_ATTRIBUTES: Byte = 0x01
    private const val TAG_JOB_ATTRIBUTES: Byte = 0x02
    private const val TAG_END_OF_ATTRIBUTES: Byte = 0x03
    private const val TAG_PRINTER_ATTRIBUTES: Byte = 0x04
    
    // Value tags
    private const val TAG_INTEGER: Byte = 0x21
    private const val TAG_BOOLEAN: Byte = 0x22
    private const val TAG_ENUM: Byte = 0x23
    private const val TAG_URI: Byte = 0x45
    private const val TAG_CHARSET: Byte = 0x47
    private const val TAG_NATURAL_LANGUAGE: Byte = 0x48
    private const val TAG_MIME_MEDIA_TYPE: Byte = 0x49
    private const val TAG_KEYWORD: Byte = 0x44
    private const val TAG_TEXT_WITHOUT_LANGUAGE: Byte = 0x41
    private const val TAG_NAME_WITHOUT_LANGUAGE: Byte = 0x42

    /**
     * Create a Get-Printer-Attributes IPP request
     */
    fun createGetPrinterAttributesRequest(printerUri: String): ByteArray {
        val requestId = Random.nextInt()
        
        return ByteArrayOutputStream().use { baos ->
            // IPP header
            baos.write(IPP_VERSION_MAJOR.toInt())
            baos.write(IPP_VERSION_MINOR.toInt())
            baos.write(shortToBytes(IPPOperations.GET_PRINTER_ATTRIBUTES.toShort()))
            baos.write(intToBytes(requestId))
            
            // Operation attributes group
            baos.write(TAG_OPERATION_ATTRIBUTES.toInt())
            
            // attributes-charset
            writeAttribute(baos, TAG_CHARSET, "attributes-charset", "utf-8")
            
            // attributes-natural-language  
            writeAttribute(baos, TAG_NATURAL_LANGUAGE, "attributes-natural-language", "en-us")
            
            // printer-uri
            writeAttribute(baos, TAG_URI, "printer-uri", printerUri)
            
            // requested-attributes (basic set for testing)
            writeAttribute(baos, TAG_KEYWORD, "requested-attributes", "printer-name")
            writeAttribute(baos, TAG_KEYWORD, null, "printer-state")
            writeAttribute(baos, TAG_KEYWORD, null, "printer-state-reasons")
            writeAttribute(baos, TAG_KEYWORD, null, "document-format-supported")
            
            // End of attributes
            baos.write(TAG_END_OF_ATTRIBUTES.toInt())
            
            baos.toByteArray()
        }
    }

    /**
     * Create a Print-Job IPP request
     */
    fun createPrintJobRequest(
        printerUri: String,
        documentData: ByteArray,
        documentFormat: String,
        jobName: String,
        copies: Int
    ): ByteArray {
        val requestId = Random.nextInt()
        
        return ByteArrayOutputStream().use { baos ->
            // IPP header
            baos.write(IPP_VERSION_MAJOR.toInt())
            baos.write(IPP_VERSION_MINOR.toInt())
            baos.write(shortToBytes(IPPOperations.PRINT_JOB.toShort()))
            baos.write(intToBytes(requestId))
            
            // Operation attributes group
            baos.write(TAG_OPERATION_ATTRIBUTES.toInt())
            
            // attributes-charset
            writeAttribute(baos, TAG_CHARSET, "attributes-charset", "utf-8")
            
            // attributes-natural-language  
            writeAttribute(baos, TAG_NATURAL_LANGUAGE, "attributes-natural-language", "en-us")
            
            // printer-uri
            writeAttribute(baos, TAG_URI, "printer-uri", printerUri)
            
            // requesting-user-name
            writeAttribute(baos, TAG_NAME_WITHOUT_LANGUAGE, "requesting-user-name", "scanbridge-user")
            
            // job-name
            writeAttribute(baos, TAG_NAME_WITHOUT_LANGUAGE, "job-name", jobName)
            
            // document-format
            writeAttribute(baos, TAG_MIME_MEDIA_TYPE, "document-format", documentFormat)
            
            // Job attributes group
            baos.write(TAG_JOB_ATTRIBUTES.toInt())
            
            // copies
            if (copies > 1) {
                writeIntegerAttribute(baos, "copies", copies)
            }
            
            // End of attributes
            baos.write(TAG_END_OF_ATTRIBUTES.toInt())
            
            // Document data
            baos.write(documentData)
            
            baos.toByteArray()
        }
    }

    /**
     * Parse an IPP response
     */
    fun parseResponse(data: ByteArray): IPPResponse {
        if (data.size < 8) {
            throw IllegalArgumentException("Invalid IPP response: too short")
        }
        
        val buffer = ByteBuffer.wrap(data)
        
        // Read IPP header
        val versionMajor = buffer.get()
        val versionMinor = buffer.get()
        val statusCode = buffer.short.toInt() and 0xFFFF
        val requestId = buffer.int
        
        Timber.tag(TAG).d("IPP Response: version=$versionMajor.$versionMinor, status=$statusCode, requestId=$requestId")
        
        val attributes = mutableMapOf<String, Any>()
        
        // Parse attributes
        while (buffer.hasRemaining()) {
            val tag = buffer.get()
            
            when (tag) {
                TAG_END_OF_ATTRIBUTES -> {
                    // Remaining data is document content
                    val remainingData = if (buffer.hasRemaining()) {
                        val remaining = ByteArray(buffer.remaining())
                        buffer.get(remaining)
                        remaining
                    } else null
                    
                    return IPPResponse(
                        statusCode = statusCode,
                        statusMessage = IPPStatusCodes.getStatusMessage(statusCode),
                        attributes = attributes,
                        data = remainingData
                    )
                }
                
                TAG_OPERATION_ATTRIBUTES, TAG_JOB_ATTRIBUTES, TAG_PRINTER_ATTRIBUTES -> {
                    // Start of attribute group - continue reading
                    continue
                }
                
                else -> {
                    // Read attribute
                    try {
                        val attribute = readAttribute(buffer, tag)
                        if (attribute != null) {
                            attributes[attribute.first] = attribute.second
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Failed to parse attribute with tag $tag")
                        break
                    }
                }
            }
        }
        
        return IPPResponse(
            statusCode = statusCode,
            statusMessage = IPPStatusCodes.getStatusMessage(statusCode),
            attributes = attributes
        )
    }

    private fun writeAttribute(baos: ByteArrayOutputStream, valueTag: Byte, name: String?, value: String) {
        baos.write(valueTag.toInt())
        
        if (name != null) {
            val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
            baos.write(shortToBytes(nameBytes.size.toShort()))
            baos.write(nameBytes)
        } else {
            baos.write(shortToBytes(0))
        }
        
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)
        baos.write(shortToBytes(valueBytes.size.toShort()))
        baos.write(valueBytes)
    }
    
    private fun writeIntegerAttribute(baos: ByteArrayOutputStream, name: String, value: Int) {
        baos.write(TAG_INTEGER.toInt())
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        baos.write(shortToBytes(nameBytes.size.toShort()))
        baos.write(nameBytes)
        baos.write(shortToBytes(4)) // Integer is 4 bytes
        baos.write(intToBytes(value))
    }

    private fun readAttribute(buffer: ByteBuffer, valueTag: Byte): Pair<String, Any>? {
        if (!buffer.hasRemaining()) return null
        
        val nameLength = buffer.short.toInt() and 0xFFFF
        val name = if (nameLength > 0) {
            val nameBytes = ByteArray(nameLength)
            buffer.get(nameBytes)
            String(nameBytes, StandardCharsets.UTF_8)
        } else {
            return null // Skip attributes without names for now
        }
        
        val valueLength = buffer.short.toInt() and 0xFFFF
        val valueBytes = ByteArray(valueLength)
        buffer.get(valueBytes)
        
        val value = when (valueTag) {
            TAG_INTEGER -> {
                if (valueBytes.size == 4) {
                    ByteBuffer.wrap(valueBytes).int
                } else valueBytes
            }
            TAG_BOOLEAN -> valueBytes.isNotEmpty() && valueBytes[0] != 0.toByte()
            TAG_ENUM -> {
                if (valueBytes.size == 4) {
                    ByteBuffer.wrap(valueBytes).int
                } else valueBytes
            }
            else -> String(valueBytes, StandardCharsets.UTF_8)
        }
        
        return name to value
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() shr 8).toByte(),
            value.toByte()
        )
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
}