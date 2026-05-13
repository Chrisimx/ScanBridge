package io.github.chrisimx.scanbridge.repositories

import androidx.datastore.core.DataStore
import io.github.chrisimx.scanbridge.BuildConfig
import io.github.chrisimx.scanbridge.ShownMessagesRepository
import io.github.chrisimx.scanbridge.UserInformationMessage
import io.github.chrisimx.scanbridge.proto.ShownMessages
import io.github.chrisimx.scanbridge.proto.copy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.InjectedParam

class DatastoreShownMessagesRepository(
    val shownMessagesDataStore: DataStore<ShownMessages>,
    @InjectedParam
    val coroutineScope: CoroutineScope
) : ShownMessagesRepository {
    override fun getWasShownFlow(message: UserInformationMessage): Flow<Boolean> = shownMessagesDataStore.data.map {
        (BuildConfig.FLAVOR != "play" && message.playOnly) || !when (message) {
            UserInformationMessage.THANKS_FOR_PURCHASE -> it.thankPlayOne
        }
    }

    override suspend fun setShown(message: UserInformationMessage, shown: Boolean) {
        shownMessagesDataStore.updateData {
            it.copy {
                when (message) {
                    UserInformationMessage.THANKS_FOR_PURCHASE -> {
                        thankPlayOne = shown
                    }
                }
            }
        }
    }
}
