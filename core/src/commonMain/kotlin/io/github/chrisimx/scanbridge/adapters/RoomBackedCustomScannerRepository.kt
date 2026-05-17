package io.github.chrisimx.scanbridge.adapters

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.ports.CustomScannerRepository
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

class RoomBackedCustomScannerRepository(
    appDb: ScanBridgeDb,
) : CustomScannerRepository {
    private val customScannerDao = appDb.customScannerDao()
    override fun allFlow(): Flow<List<CustomScanner>> = customScannerDao.getAllFlow()

    override suspend fun add(scanner: CustomScanner) {
        customScannerDao.insertAll(scanner)
    }

    override suspend fun getById(id: Uuid): CustomScanner? {
        return customScannerDao.getById(id)
    }

    override suspend fun deleteById(id: Uuid) {
        customScannerDao.deleteById(id)
    }
}
