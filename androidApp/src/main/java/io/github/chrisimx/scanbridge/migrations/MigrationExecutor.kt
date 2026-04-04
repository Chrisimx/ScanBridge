package io.github.chrisimx.scanbridge.migrations

interface MigrationExecutor {
    suspend fun runMigrations()
}
