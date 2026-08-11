package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.filesystem.FileSystem
import io.github.chrisimx.scanbridge.model.ScanBridgeFile

class DeleteScanUseCase(
    val fileSystem: FileSystem,
    scanBridgeDb: ScanBridgeDb
) {
    private val scannedPageDao = scanBridgeDb.scannedPageDao()
    suspend fun deleteScan(scan: ScannedPage) {
        scannedPageDao.delete(scan)

        val scannedPageFile = ScanBridgeFile(scan.filePath)
        fileSystem.delete(scannedPageFile)
    }
}
