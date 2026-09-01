package io.github.chrisimx.scanbridge.scanning

interface ScanExecutor {
    suspend fun executeScans()
}
