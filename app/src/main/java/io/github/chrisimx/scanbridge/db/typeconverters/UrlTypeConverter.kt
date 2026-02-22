package io.github.chrisimx.scanbridge.db.typeconverters

import androidx.room.TypeConverter
import io.ktor.http.Url

class UrlTypeConverter {
    @TypeConverter
    fun fromUrlString(urlString: String): Url = Url(urlString)
    @TypeConverter
    fun toUrlString(url: Url): String = url.toString()
}
