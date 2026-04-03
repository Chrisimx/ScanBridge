package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class UuidTypeConverter {
    @TypeConverter
    fun fromUUIDString(value: String): Uuid = Uuid.parse(value)

    @TypeConverter
    fun uuidToString(uuid: Uuid): String = uuid.toString()
}
