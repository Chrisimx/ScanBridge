package io.github.chrisimx.scanbridge.scanning

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.filesystem.FileSystem
import io.github.chrisimx.scanbridge.model.ScanBridgeFile
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DeleteSessionUseCase(
    val fileSystem: FileSystem,
    val db: ScanBridgeDb
) {
    val sessionDao = db.sessionDao()
    val scannedPageDao = db.scannedPageDao()
    val tmpFileDao = db.tmpFileDao()

    suspend fun deleteSession(sessionId: Uuid) {
        val filePaths = mutableListOf<ScanBridgeFile>()
        val tmpPaths = mutableListOf<ScanBridgeFile>()

        db.useWriterConnection {
            it.immediateTransaction {
                val scannedPages = scannedPageDao.getAllForSession(sessionId)
                val tmpFiles = tmpFileDao.getFilesBySessionId(sessionId)

                filePaths += scannedPages.map { ScanBridgeFile(it.filePath) }
                tmpPaths += tmpFiles.map { ScanBridgeFile(it.path) }

                sessionDao.deleteById(sessionId)
            }
        }

        withContext(Dispatchers.IO) {
            filePaths.forEach {
                fileSystem.delete(it, mustExist = false)
            }
            tmpPaths.forEach {
                fileSystem.delete(it, mustExist = false)
            }
        }
    }
}
