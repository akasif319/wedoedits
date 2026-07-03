package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, TimelineItem::class.java)
    private val adapter = moshi.adapter<List<TimelineItem>>(listType)

    @TypeConverter
    fun fromTimelineItemsJson(json: String): List<TimelineItem> {
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toTimelineItemsJson(items: List<TimelineItem>): String {
        return try {
            adapter.toJson(items)
        } catch (e: Exception) {
            "[]"
        }
    }
}
