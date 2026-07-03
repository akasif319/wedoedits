package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val aspectRatio: String = "9:16", // "9:16", "16:9", "1:1"
    val itemsJson: String // Serialized List<TimelineItem>
)
