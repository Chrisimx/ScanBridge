package io.github.chrisimx.scanbridge.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.chrisimx.scanbridge.buildinfo.BuildInfoProvider
import io.github.chrisimx.scanbridge.model.Platform
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.startupmessages.ShownStartupMessagesRepository
import io.github.chrisimx.scanbridge.startupmessages.StartupMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

class RoomMigrationV5To6(val buildInfoProvider: BuildInfoProvider, val koinScope: Scope, val loggerFactory: ScanBridgeLoggerFactory) :
    Migration(5, 6) {

    private val logger = loggerFactory.withClass(this::class)

    override fun migrate(connection: SQLiteConnection): Unit = runBlocking {
        val oldShownMessages = if (buildInfoProvider.platform == Platform.ANDROID) {
            // On Android, we need to get the old ShownStartupMessages from the DataStore
            logger.debug { "Android: Migration of old shown startup messages from DataStore to Room" }
            val oldDataStoreRepo = koinScope.get<ShownStartupMessagesRepository>(named("legacyDatastoreShownMessages"))

            StartupMessage.entries.map {
                it to oldDataStoreRepo.getWasShownFlow(it).first()
            }.filter { (_, wasShown) -> wasShown }
                .map { (message, _) -> message }
        } else {
            logger.debug { "Other Platform (not Android): Migration of old shown startup messages from DataStore to Room not necessary" }
            null
        }

        connection.execSQL("CREATE TABLE IF NOT EXISTS `shownstartupmessages` (`message` TEXT NOT NULL, PRIMARY KEY(`message`))")

        oldShownMessages?.forEach { message ->
            connection.execSQL("INSERT INTO shownstartupmessages (message) VALUES ('${message.name}')")
        }
    }
}
