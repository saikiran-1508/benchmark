package com.example.benchmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val duration: String,     // e.g. "1h", "30m"
    val startTime: String,    // e.g. "6:00 PM"
    val day: String,          // Stores "2025-12-18"
    val isCompleted: Boolean = false,
    val isImportant: Boolean = false, // Starred tasks show up in the Focus tab
    val soundUri: String? = null // Custom ringtone picked for the reminder
)
