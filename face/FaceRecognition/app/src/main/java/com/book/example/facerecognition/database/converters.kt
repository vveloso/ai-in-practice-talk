package com.book.example.facerecognition.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.json.Json

class FloatArrayConverters {
    private val floatStrategy = FloatArraySerializer()

    @TypeConverter
    fun fromFloatArray(value : FloatArray): String =
        Json.encodeToString(floatStrategy, value)

    @TypeConverter
    fun toFloatArray(value: String): FloatArray =
        Json.decodeFromString(floatStrategy, value)
}