package com.example.benchmark.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Return a list of TaskEntity (the new class name)
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Insert a TaskEntity
    @Insert
    suspend fun insertTask(task: TaskEntity)
}