package io.github.chrisimx.scanbridge.startupmessages

import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ShownStartupMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomShownStartupMessagesRepository(scanBridgeDb: ScanBridgeDb, private val buildInfoProvider: BuildInfoProvider) :
    ShownStartupMessagesRepository {
    private val shownMessagesDao = scanBridgeDb.shownStartupMessageDao()

    override fun getWasShownFlow(message: StartupMessage): Flow<Boolean> = shownMessagesDao.exists(message).map {
        it || !message.editions.contains(buildInfoProvider.edition)
    }

    override suspend fun setShown(message: StartupMessage, shown: Boolean) {
        if (shown) {
            shownMessagesDao.insertAll(
                ShownStartupMessage(message)
            )
        } else {
            shownMessagesDao.delete(ShownStartupMessage(message))
        }
    }
}
