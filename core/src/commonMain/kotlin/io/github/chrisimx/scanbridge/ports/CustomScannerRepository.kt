package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

interface CustomScannerRepository {
    fun allFlow(): Flow<List<CustomScanner>>
    suspend fun add(scanner: CustomScanner)

    suspend fun getById(id: Uuid): CustomScanner?

    suspend fun deleteById(id: Uuid)
}
