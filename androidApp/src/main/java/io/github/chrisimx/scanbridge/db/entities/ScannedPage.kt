package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ScanRelativeRotation
import kotlin.uuid.Uuid

@Entity(
    tableName = "scannedpages",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["sessionId"],
            childColumns = ["ownerSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("ownerSessionId"),
        Index(
            value = ["ownerSessionId", "orderIndex"],
            unique = true
        )
    ]
)
data class ScannedPage(
    @PrimaryKey
    val scanId: Uuid,
    val ownerSessionId: Uuid,
    val filePath: String,
    val originalScanSettings: ScanSettings,
    val rotation: ScanRelativeRotation = ScanRelativeRotation.Original,
    val orderIndex: Int
)
