package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete

class DeleteScanUseCase(
    scanBridgeDb: ScanBridgeDb
) {
    private val scannedPageDao = scanBridgeDb.scannedPageDao()
    suspend fun deleteScan(scan: ScannedPage) {
        scannedPageDao.delete(scan)

        val scannedPageFile = PlatformFile(scan.filePath)
        scannedPageFile.delete()
    }
}
