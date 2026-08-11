package io.github.chrisimx.scanbridge.scanning

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ScannedPage

class SwapPagesUseCase(
    val scanBridgeDb: ScanBridgeDb
) {
    val scannedPageDao = scanBridgeDb.scannedPageDao()

    suspend operator fun invoke(page1: ScannedPage, page2: ScannedPage) {
        scannedPageDao.swapPages(page1, page2)
    }
}
