package com.example.benchmark.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity( // Renamed to TaskEntity for clarity
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val duration: String,     // Changed from 'estimatedTime' to match our App code
    val startTime: String,
    val day: String,          // Stores "2025-12-18"
    val isCompleted: Boolean = false
)