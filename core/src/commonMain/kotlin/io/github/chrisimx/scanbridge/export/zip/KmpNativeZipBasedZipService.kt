package io.github.chrisimx.scanbridge.export.zip

import io.github.chrisimx.kmpnativezip.ZipEntry
import io.github.chrisimx.kmpnativezip.ZipWriter
import io.github.chrisimx.kmpnativezip.compression.impl.DeflateCompressionAlgorithm
import io.github.chrisimx.kmpnativezip.compression.impl.StoreCompressionAlgorithm
import io.github.chrisimx.kmpnativezip.dostime.DOSDate
import io.github.chrisimx.kmpnativezip.dostime.DOSTime
import io.github.chrisimx.kmpnativezip.io.seekable.seekableSink
import io.github.chrisimx.kmpnativezip.zipprimitives.CompressionMethod
import io.github.chrisimx.scanbridge.model.FileToBeExported
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.source
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path

class KmpNativeZipBasedZipService() : ZipService {
    override fun zipFlat(
        files: List<FileToBeExported>,
        output: PlatformFile,
        mapFileName: (PlatformFile) -> String
    ) {
        val outputFile = Path(output.path).seekableSink()

        val exportTime = Clock.System.now()

        outputFile.use {
            ZipWriter(
                outputFile,
                listOf(StoreCompressionAlgorithm(), DeflateCompressionAlgorithm())
            ).use { writer ->
                files.forEach { fileExportInfo ->
                    val file = fileExportInfo.file
                    val fileName = fileExportInfo.exportedFileName

                    // Check filename
                    require('/' !in fileName) { "Filename must not contain a slash for zipFlat" }

                    val zipEntry = zipEntryWithFilename(fileName, exportTime)

                    writer.addFile(zipEntry).use { zipSink ->
                        file.source().buffered().use { originalFileSource ->
                            originalFileSource.transferTo(zipSink)
                        }
                    }

                    writer.finishEntry()
                }
            }
        }
    }

    private fun zipEntryWithFilename(filename: String, exportTime: Instant): ZipEntry {
        val exportLocaleTime = exportTime.toLocalDateTime(TimeZone.currentSystemDefault())

        val dosTime = DOSTime(
            exportLocaleTime.second.toUInt(),
            exportLocaleTime.minute.toUInt(),
            exportLocaleTime.hour.toUInt()
        )

        val dosDate = DOSDate(
            exportLocaleTime.day.toUInt(),
            exportLocaleTime.month.number.toUInt(),
            exportLocaleTime.year.toUInt()
        )

        return ZipEntry(
            compressionMethod = CompressionMethod.STORED,
            lastModifiedDate = dosDate,
            lastModifiedTime = dosTime,
            fileName = filename,
            extraFields = emptyList()
        )
    }
}
