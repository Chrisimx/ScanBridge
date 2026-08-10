package io.github.chrisimx.scanbridge.filesystem

import io.github.chrisimx.scanbridge.model.ScanBridgeFile

interface FileSystem {
    suspend fun delete(
        file: ScanBridgeFile,
        mustExist: Boolean = true
    )
}
