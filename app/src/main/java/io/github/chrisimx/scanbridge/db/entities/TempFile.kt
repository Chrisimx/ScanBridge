package io.github.chrisimx.scanbridge.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.uuid.Uuid

@Entity(
    tableName = "tempfiles",
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
data class TempFile(
    @PrimaryKey
    val tempFileId: Uuid,
    val ownerSessionId: Uuid,
    val path: String
)
