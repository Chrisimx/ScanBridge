package io.github.chrisimx.scanbridge.migrations

import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.ExecutedMigrationToRoom
import io.github.chrisimx.scanbridge.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatform.getKoin

class RoomBackedMigrationExecutor(val db: ScanBridgeDb) : MigrationExecutor {
    val logger by logger<RoomBackedMigrationExecutor>()
    val executedMigrationsDao = db.executedMigrationsDao()
    override suspend fun runMigrations() {
        val migrations = getKoin().getAll<Migration>()

        migrations.forEach {
            withContext(Dispatchers.IO) {
                val ranAlready = executedMigrationsDao.isAlreadyDone(it.migrationId)
                if (!ranAlready) {
                    val success = it.migrate(db)
                    if (success) {
                        executedMigrationsDao.markAsExecuted(
                            ExecutedMigrationToRoom(it.migrationId)
                        )
                        logger.info { "Migration with id ${it.migrationId} has finished successfully." }
                    } else {
                        logger.error { "Migration with id ${it.migrationId} has failed" }
                    }
                } else {
                    logger.debug { "Migration with id ${it.migrationId} has already run" }
                }
            }
        }
    }
}
