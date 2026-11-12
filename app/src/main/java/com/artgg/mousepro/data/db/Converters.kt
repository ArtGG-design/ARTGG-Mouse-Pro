package com.artgg.mousepro.data.db

import androidx.room.TypeConverter
import com.artgg.mousepro.data.model.MappingComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMappingComponentList(value: List<MappingComponent>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toMappingComponentList(value: String): List<MappingComponent> =
        try { json.decodeFromString(value) } catch (_: Exception) { emptyList() }

    @TypeConverter
    fun fromIntList(value: List<Int>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        try { json.decodeFromString(value) } catch (_: Exception) { emptyList() }
}