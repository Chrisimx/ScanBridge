package io.github.chrisimx.scanbridge.model

sealed class ScanningError(val unlocalizedMessage: String) {
    data class UnsupportedContentType(val contentType: String) : ScanningError(
        "Unsupported content type: $contentType"
    )
    data class InvalidScanHandle(val handle: ScannerHandle) : ScanningError(
        "Invalid scan handle: $handle"
    )
    data class JobCreationFailed(val creationError: String) : ScanningError(
        "Job creation failed: $creationError"
    )

    data class JobCompletedInFailedState(val jobStatus: String) : ScanningError(
        "Job completed but failed. Job status: $jobStatus"
    )

    data object PollingTimedOut: ScanningError(
        "Timed out waiting for images to transfer"
    )

    data class NextPageRetrievalError(val error: String, val jobStatus: String) : ScanningError(
        "Error retrieving next page: $error. Job status: $jobStatus"
    )

    data class Other(val message: String) : ScanningError(message)

}
