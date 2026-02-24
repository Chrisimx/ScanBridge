package io.github.chrisimx.scanbridge.services

import io.github.chrisimx.scanbridge.data.model.ScanJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class ScanJobEvent {
    data class Completed(val job: ScanJob) : ScanJobEvent()
    data class Failed(val job: ScanJob, val reason: String) : ScanJobEvent()
    data class Started(val job: ScanJob) : ScanJobEvent()
}

class ScanJobRepository {
    // Channel acts as an async queue
    private val jobChannel = Channel<ScanJob>(capacity = Channel.UNLIMITED)

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    val jobFlow = jobChannel.receiveAsFlow()

    private val _events = MutableSharedFlow<ScanJobEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ScanJobEvent> = _events.asSharedFlow()

    private val _isJobRunning = MutableStateFlow(false)
    val isJobRunning = _isJobRunning.asStateFlow()

    private val _shouldCancel = MutableStateFlow(false)
    val shouldCancel = _shouldCancel.asStateFlow()

    /**
     * Enqueue a ScanJob
     */
    fun enqueue(job: ScanJob) {
        coroutineScope.launch {
            jobChannel.send(job)
            Timber.d("Enqueued job: $job")
        }
    }

    fun setJobRunning(value: Boolean) {
        Timber.d("setJobRunning($value)")
        _isJobRunning.update {
            value
        }
    }

    fun setCancel(value: Boolean) {
        Timber.d("setCancel($value)")
        _shouldCancel.update {
            value
        }
    }

    /**
     * Notify that a job was completed
     */
    fun notifyCompleted(job: ScanJob) {
        Timber.d("notifyCompleted($job)")
        coroutineScope.launch {
            _events.emit(ScanJobEvent.Completed(job))
        }
    }

    /**
     * Notify that a job was started
     */
    fun notifyStarted(job: ScanJob) {
        Timber.d("notifyStarted($job)")
        coroutineScope.launch {
            _events.emit(ScanJobEvent.Started(job))
        }
    }

    /**
     * Notify that a job failed
     */
    fun notifyFailed(job: ScanJob, reason: String) {
        Timber.d("notifyFailed($job, $reason)")
        coroutineScope.launch {
            _events.emit(ScanJobEvent.Failed(job, reason))
        }
    }

    /**
     * Consume the next job
     */
    fun nextJob(): ScanJob? = jobChannel.tryReceive().getOrNull()

    /**
     * Clear the queue
     */
    fun clear() {
        jobChannel.cancel() // closes channel, all pending receivers fail
    }
}
