package io.github.chrisimx.scanbridge.scanning

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DeleteSessionUseCase(
    val db: ScanBridgeDb
) {
    val sessionDao = db.sessionDao()
    val scannedPageDao = db.scannedPageDao()
    val tmpFileDao = db.tmpFileDao()

    suspend fun deleteSession(sessionId: Uuid) {
        val filePaths = mutableListOf<PlatformFile>()
        val tmpPaths = mutableListOf<PlatformFile>()

        db.useWriterConnection {
            it.immediateTransaction {
                val scannedPages = scannedPageDao.getAllForSession(sessionId)
                val tmpFiles = tmpFileDao.getFilesBySessionId(sessionId)

                filePaths += scannedPages.map { PlatformFile(it.filePath) }
                tmpPaths += tmpFiles.map { PlatformFile(it.path) }

                sessionDao.deleteById(sessionId)
            }
        }

        withContext(Dispatchers.IO) {
            filePaths.forEach {
                it.delete(mustExist = false)
            }
            tmpPaths.forEach {
                it.delete(mustExist = false)
            }
        }
    }
}
