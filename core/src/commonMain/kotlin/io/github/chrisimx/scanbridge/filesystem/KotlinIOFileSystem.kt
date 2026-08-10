package io.github.chrisimx.scanbridge.filesystem

import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemFileSystem

class KotlinIOFileSystem : FileSystem {
    override suspend fun delete(file: ScanBridgeFile, mustExist: Boolean) = withContext(Dispatchers.IO) {
        SystemFileSystem.delete(file.path, mustExist)
    }
}
