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

    // Holds the list of tasks to show on screen
    val tasks = dao.getAllTasks()

    fun addTask(name: String, time: String) {
        viewModelScope.launch {
            dao.insertTask(Task(name = name, estimatedTime = time))
        }
    }
}