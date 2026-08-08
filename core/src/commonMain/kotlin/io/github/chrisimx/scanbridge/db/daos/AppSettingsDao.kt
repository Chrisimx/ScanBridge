package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.chrisimx.scanbridge.db.entities.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM appsettings WHERE id = 1")
    suspend fun getAppSettings(): AppSettings?

    @Query("SELECT * FROM appsettings WHERE id = 1")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppSettings(appSettings: AppSettings)

    @Transaction
    suspend fun updateAppSettings(updateOperation: AppSettings.() -> AppSettings) {
        val appSettings = getAppSettings() ?: AppSettings()
        setAppSettings(appSettings.updateOperation())
    }

    @Query("DELETE FROM appsettings WHERE id = 1")
    suspend fun clearAppSettings()
}
