package com.example.benchmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val estimatedTime: String,
    val startTime: String,
    val day: String,          // NEW: Stores "M", "T", "W", etc.
    val isCompleted: Boolean = false
)