package io.github.chrisimx.scanbridge

import kotlinx.coroutines.flow.Flow

interface ShownMessagesRepository {
    fun getWasShownFlow(message: UserInformationMessage): Flow<Boolean>
    suspend fun setShown(message: UserInformationMessage, shown: Boolean)
}

enum class UserInformationMessage(val playOnly: Boolean) {
    THANKS_FOR_PURCHASE(true)
}
