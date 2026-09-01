package io.github.chrisimx.scanbridge.scanning

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.filesystem.StandardDirectoryProvider
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.model.ScanJob
import io.github.chrisimx.scanbridge.model.ScanRelativeRotation
import io.github.chrisimx.scanbridge.model.ScanningError
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.protocol.ScanJobProcessingEvent
import io.github.chrisimx.scanbridge.util.extractPdfImages
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.write
import kotlin.getValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CommonScanExecutor(
    loggerFactory: ScanBridgeLoggerFactory,
    private val scanJobs: ScanJobRepository,
    private val db: ScanBridgeDb,
    private val httpClientFactory: HttpClientFactory,
    private val standardDirectoryProvider: StandardDirectoryProvider
) : ScanExecutor {
    private val scannedPageDao: ScannedPageDao = db.scannedPageDao()

    private val logger = loggerFactory.withClass(this::class)

    override suspend fun executeScans() {
        try {
            scanJobs.setJobRunning(true)
            var job = scanJobs.nextJob()
            while (job != null) {
                doScan(job)
                job = scanJobs.nextJob()
            }
        } finally {
            scanJobs.setJobRunning(false)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun doScan(scanJob: ScanJob) {
        scanJobs.notifyStarted(scanJob)

        val scanningProtocol = scanJob.scannerHandle.protocol
        val scanningFlow = scanningProtocol.executeScanJob(
            scanJob.scannerHandle,
            scanJob.connectionSettings,
            scanJob.scanSettings,
            scanJobs.shouldCancel
        )

        var failed = false

        var pageCounter = 1

        scanningFlow.collect { processingEvent ->
            when (processingEvent) {
                ScanJobProcessingEvent.Cancelled -> scanJobs.setCancel(false)

                is ScanJobProcessingEvent.Failure -> {
                    failed = true
                    scanJobs.notifyFailed(scanJob, processingEvent.error)
                    return@collect
                }

                is ScanJobProcessingEvent.NewPage -> {
                    val pageData = processingEvent.scannedPage

                    val scanPageFileName = "scan-" + Uuid.random().toString()

                    val scanPageFile = PlatformFile(standardDirectoryProvider.appFilesDirectory, scanPageFileName)

                    scanPageFile.write(pageData.data)

                    when (pageData.contentType) {
                        // TODO: Extract this conversion to a service
                        "image/jpeg" -> {
                            addScan(
                                scanJob.ownerSessionId,
                                scanPageFile.absolutePath(),
                                scanJob.scanSettings,
                                ScanRelativeRotation.Original,
                                "scan-${pageCounter.toString().padStart(4, '0')}.jpg"
                            )
                        }

                        "application/pdf" -> {
                            val extractedImages = extractPdfImages(
                                scanPageFile,
                                scanPageFile.parent()!!
                            )

                            extractedImages.forEach {
                                addScan(scanJob.ownerSessionId, it, scanJob.scanSettings, ScanRelativeRotation.Original)
                            }
                        }

                        else -> {
                            failed = true
                            scanJobs.notifyFailed(scanJob, ScanningError.UnsupportedContentType(pageData.contentType))
                            return@collect
                        }
                    }

                    pageCounter++
                }
            }
        }

        if (failed) return

        scanJobs.notifyCompleted(scanJob)
    }

    suspend fun addScan(
        sessionID: Uuid,
        path: String,
        settings: CommonScanSettings,
        rotation: ScanRelativeRotation,
        fileName: String? = null
    ) {
        logger.debug { "Adding scan: $path, $rotation" }
        db.useWriterConnection {
            it.immediateTransaction {
                val highestIdx = scannedPageDao.getHighestIdxForSession(sessionID) ?: -1

                logger.debug { "Inserting scan with index ${highestIdx + 1}" }
                scannedPageDao.insertAll(
                    ScannedPage(
                        Uuid.generateV4(),
                        sessionID,
                        path,
                        settings,
                        rotation,
                        highestIdx + 1,
                        fileName
                    )
                )
            }
        }
    }
}
