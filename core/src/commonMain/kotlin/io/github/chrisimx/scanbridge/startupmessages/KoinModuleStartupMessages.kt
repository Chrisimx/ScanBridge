package io.github.chrisimx.scanbridge.startupmessages

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_STARTUP_MESSAGES = module {
    single<RoomShownStartupMessagesRepository>() bind ShownStartupMessagesRepository::class
}
