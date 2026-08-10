package io.github.chrisimx.scanbridge.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.proto.copy
import io.github.chrisimx.scanbridge.startupmessages.ShownStartupMessagesRepository
import io.github.chrisimx.scanbridge.startupmessages.StartupMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatastoreShownMessagesRepository(context: Context, val buildInfoProvider: BuildInfoProvider) :
    ShownStartupMessagesRepository {
    val shownMessagesDataStore = context.shownMessagesStore

    override fun getWasShownFlow(message: StartupMessage): Flow<Boolean> = shownMessagesDataStore.data.map {
        !message.editions.contains(buildInfoProvider.edition) || when (message) {
            StartupMessage.THANKS_FOR_PURCHASE -> it.thankPlayOne
        }
    }

    override suspend fun setShown(message: StartupMessage, shown: Boolean) {
        shownMessagesDataStore.updateData {
            it.copy {
                when (message) {
                    StartupMessage.THANKS_FOR_PURCHASE -> {
                        thankPlayOne = shown
                    }
                }
            }
        }
    }
}
