package io.github.chrisimx.scanbridge.scannerdiscovery

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

class RoomBackedCustomScannerRepository(appDb: ScanBridgeDb) : CustomScannerRepository {
    private val customScannerDao = appDb.customScannerDao()
    override fun allFlow(): Flow<List<CustomScanner>> = customScannerDao.getAllFlow()

    override suspend fun add(scanner: CustomScanner) {
        customScannerDao.insertAll(scanner)
    }

    override suspend fun getById(id: Uuid): CustomScanner? = customScannerDao.getById(id)

    override suspend fun deleteById(id: Uuid) {
        customScannerDao.deleteById(id)
    }
}
