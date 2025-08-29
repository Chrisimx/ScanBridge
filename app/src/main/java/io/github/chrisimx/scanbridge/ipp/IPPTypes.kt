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

/**
 * IPP Response representing parsed response from an IPP operation
 */
data class IPPResponse(
    val statusCode: Int,
    val statusMessage: String,
    val attributes: Map<String, Any> = emptyMap(),
    val data: ByteArray? = null
) {
    val isSuccessful: Boolean
        get() = statusCode in 0x0000..0x00FF // Successful status codes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IPPResponse

        if (statusCode != other.statusCode) return false
        if (statusMessage != other.statusMessage) return false
        if (attributes != other.attributes) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * IPP operation codes
 */
object IPPOperations {
    const val PRINT_JOB = 0x0002
    const val GET_PRINTER_ATTRIBUTES = 0x000B
    const val GET_JOB_ATTRIBUTES = 0x0009
    const val CANCEL_JOB = 0x0008
}

/**
 * IPP status codes
 */
object IPPStatusCodes {
    const val SUCCESSFUL_OK = 0x0000
    const val SUCCESSFUL_OK_IGNORED_OR_SUBSTITUTED_ATTRIBUTES = 0x0001
    const val SUCCESSFUL_OK_CONFLICTING_ATTRIBUTES = 0x0002
    
    const val CLIENT_ERROR_BAD_REQUEST = 0x0400
    const val CLIENT_ERROR_FORBIDDEN = 0x0401
    const val CLIENT_ERROR_NOT_AUTHENTICATED = 0x0402
    const val CLIENT_ERROR_NOT_AUTHORIZED = 0x0403
    const val CLIENT_ERROR_NOT_POSSIBLE = 0x0404
    const val CLIENT_ERROR_TIMEOUT = 0x0405
    const val CLIENT_ERROR_NOT_FOUND = 0x0406
    const val CLIENT_ERROR_GONE = 0x0407
    const val CLIENT_ERROR_REQUEST_ENTITY_TOO_LARGE = 0x0408
    const val CLIENT_ERROR_REQUEST_VALUE_TOO_LONG = 0x0409
    const val CLIENT_ERROR_DOCUMENT_FORMAT_NOT_SUPPORTED = 0x040A
    const val CLIENT_ERROR_ATTRIBUTES_OR_VALUES_NOT_SUPPORTED = 0x040B
    const val CLIENT_ERROR_URI_SCHEME_NOT_SUPPORTED = 0x040C
    const val CLIENT_ERROR_CHARSET_NOT_SUPPORTED = 0x040D
    const val CLIENT_ERROR_CONFLICTING_ATTRIBUTES = 0x040E
    
    const val SERVER_ERROR_INTERNAL_ERROR = 0x0500
    const val SERVER_ERROR_OPERATION_NOT_SUPPORTED = 0x0501
    const val SERVER_ERROR_SERVICE_UNAVAILABLE = 0x0502
    const val SERVER_ERROR_VERSION_NOT_SUPPORTED = 0x0503
    const val SERVER_ERROR_DEVICE_ERROR = 0x0504
    const val SERVER_ERROR_TEMPORARY_ERROR = 0x0505
    const val SERVER_ERROR_NOT_ACCEPTING_JOBS = 0x0506
    const val SERVER_ERROR_BUSY = 0x0507
    const val SERVER_ERROR_JOB_CANCELED = 0x0508

    fun getStatusMessage(code: Int): String = when (code) {
        SUCCESSFUL_OK -> "successful-ok"
        SUCCESSFUL_OK_IGNORED_OR_SUBSTITUTED_ATTRIBUTES -> "successful-ok-ignored-or-substituted-attributes"
        SUCCESSFUL_OK_CONFLICTING_ATTRIBUTES -> "successful-ok-conflicting-attributes"
        CLIENT_ERROR_BAD_REQUEST -> "client-error-bad-request"
        CLIENT_ERROR_FORBIDDEN -> "client-error-forbidden"
        CLIENT_ERROR_NOT_AUTHENTICATED -> "client-error-not-authenticated"
        CLIENT_ERROR_NOT_AUTHORIZED -> "client-error-not-authorized"
        CLIENT_ERROR_NOT_POSSIBLE -> "client-error-not-possible"
        CLIENT_ERROR_TIMEOUT -> "client-error-timeout"
        CLIENT_ERROR_NOT_FOUND -> "client-error-not-found"
        CLIENT_ERROR_GONE -> "client-error-gone"
        CLIENT_ERROR_REQUEST_ENTITY_TOO_LARGE -> "client-error-request-entity-too-large"
        CLIENT_ERROR_REQUEST_VALUE_TOO_LONG -> "client-error-request-value-too-long"
        CLIENT_ERROR_DOCUMENT_FORMAT_NOT_SUPPORTED -> "client-error-document-format-not-supported"
        CLIENT_ERROR_ATTRIBUTES_OR_VALUES_NOT_SUPPORTED -> "client-error-attributes-or-values-not-supported"
        CLIENT_ERROR_URI_SCHEME_NOT_SUPPORTED -> "client-error-uri-scheme-not-supported"
        CLIENT_ERROR_CHARSET_NOT_SUPPORTED -> "client-error-charset-not-supported"
        CLIENT_ERROR_CONFLICTING_ATTRIBUTES -> "client-error-conflicting-attributes"
        SERVER_ERROR_INTERNAL_ERROR -> "server-error-internal-error"
        SERVER_ERROR_OPERATION_NOT_SUPPORTED -> "server-error-operation-not-supported"
        SERVER_ERROR_SERVICE_UNAVAILABLE -> "server-error-service-unavailable"
        SERVER_ERROR_VERSION_NOT_SUPPORTED -> "server-error-version-not-supported"
        SERVER_ERROR_DEVICE_ERROR -> "server-error-device-error"
        SERVER_ERROR_TEMPORARY_ERROR -> "server-error-temporary-error"
        SERVER_ERROR_NOT_ACCEPTING_JOBS -> "server-error-not-accepting-jobs"
        SERVER_ERROR_BUSY -> "server-error-busy"
        SERVER_ERROR_JOB_CANCELED -> "server-error-job-canceled"
        else -> "unknown-status-code-$code"
    }
}