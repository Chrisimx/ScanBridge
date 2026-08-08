package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.LastUsedScanSettings

@Dao
interface LastUsedScanSettingsDao {
    @Query("SELECT * FROM lastusedscansettings WHERE id = 1")
    suspend fun getLastUsedScanSettings(): LastUsedScanSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUsedScanSettings(lastUsedScanSettings: LastUsedScanSettings)

    @Query("DELETE FROM lastusedscansettings WHERE id = 1")
    suspend fun clearLastUsedScanSettings()
}
