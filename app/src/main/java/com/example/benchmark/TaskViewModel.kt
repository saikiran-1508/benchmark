package com.example.benchmark

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.benchmark.data.AppDatabase
import com.example.benchmark.data.Task
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.taskDao()

    val tasks = dao.getAllTasks()

    // Updated to accept 'day'
    fun addTask(name: String, duration: String, startTime: String, day: String) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    name = name,
                    estimatedTime = duration,
                    startTime = startTime,
                    day = day
                )
            )
        }
    }
}