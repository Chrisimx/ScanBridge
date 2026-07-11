package io.github.chrisimx.scanbridge.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.ktor.http.Url
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(tableName = "customscanners")
data class CustomScanner(
    @PrimaryKey
    val uuid: Uuid,
    val name: String,
    val url: Url,
    @ColumnInfo(defaultValue = "eSCL")
    val protocolIdentifier: String
)
