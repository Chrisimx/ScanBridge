package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.chrisimx.esclkt.ScanSettings
import kotlin.uuid.Uuid

@Entity(
    tableName = "scanjobs",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["sessionId"],
            childColumns = ["ownerSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerSessionId")]
)
data class ScanJob(
    @PrimaryKey
    val jobID: Uuid,
    val ownerSessionId: Uuid,
    val scanSettings: ScanSettings,
)
