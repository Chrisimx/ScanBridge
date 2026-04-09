package io.github.chrisimx.scanbridge.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.chrisimx.scanbridge.db.entities.LastRoute

@Dao
interface LastRouteDao {
    @Query("SELECT * FROM lastroute WHERE id = 1")
    suspend fun getLastRoute(): LastRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastRoute(lastRoute: LastRoute)

    @Query("DELETE FROM lastroute WHERE id = 1")
    suspend fun clearLastRoute()
}
