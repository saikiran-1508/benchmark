package com.example.benchmark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // One-shot read, used by BootReceiver to restore alarms after a reboot
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    // Returns the new row id so the ViewModel can use it as the alarm request code
    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    // Used by the notification's "Mark Done" button
    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun markCompleted(taskId: Int)
}
