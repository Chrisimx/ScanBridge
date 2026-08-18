package io.github.chrisimx.scanbridge.scannerdiscovery

import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface CustomScannerRepository {
    fun allFlow(): Flow<List<CustomScanner>>
    suspend fun add(scanner: CustomScanner)

    suspend fun getById(id: Uuid): CustomScanner?

    suspend fun deleteById(id: Uuid)
}
