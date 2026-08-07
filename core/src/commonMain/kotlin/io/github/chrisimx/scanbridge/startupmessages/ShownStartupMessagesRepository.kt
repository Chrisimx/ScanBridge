package io.github.chrisimx.scanbridge.startupmessages

import kotlinx.coroutines.flow.Flow

interface ShownStartupMessagesRepository {
    fun getWasShownFlow(message: StartupMessage): Flow<Boolean>
    suspend fun setShown(message: StartupMessage, shown: Boolean)
}
