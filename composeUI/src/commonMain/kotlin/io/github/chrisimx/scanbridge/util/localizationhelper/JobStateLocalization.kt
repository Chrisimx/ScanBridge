package io.github.chrisimx.scanbridge.util.localizationhelper

import io.github.chrisimx.esclkt.JobState
import org.jetbrains.compose.resources.getString
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.job_aborted_because_error
import scanbridge.composeui.generated.resources.job_canceled
import scanbridge.composeui.generated.resources.job_completed_successfully
import scanbridge.composeui.generated.resources.job_pages_still_processed
import scanbridge.composeui.generated.resources.job_state_cannot_be_retrieved
import scanbridge.composeui.generated.resources.job_still_pending

suspend fun JobState?.toJobStateString(): String = when (this) {
    JobState.Canceled -> getString(Res.string.job_canceled)
    JobState.Aborted -> getString(Res.string.job_aborted_because_error)
    JobState.Completed -> getString(Res.string.job_completed_successfully)
    JobState.Pending -> getString(Res.string.job_still_pending)
    JobState.Processing -> getString(Res.string.job_pages_still_processed)
    null -> getString(Res.string.job_state_cannot_be_retrieved)
}
