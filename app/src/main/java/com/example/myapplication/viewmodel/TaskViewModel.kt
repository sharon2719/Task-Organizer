package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.myapplication.data.TaskDatabase
import com.example.myapplication.model.Task
import com.example.myapplication.model.Category
import com.example.myapplication.worker.ReminderWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    private val categoryDao = TaskDatabase.getDatabase(application).categoryDao()

    val tasks: LiveData<List<Task>> = taskDao.getAllTasks()
    val allCategories: LiveData<List<Category>> = categoryDao.getAllCategories().asLiveData()

    // Add a task with optional dueDate
    fun addTask(name: String, categoryId: Long?, dueDate: Long?) {
        viewModelScope.launch {
            try {
                val task = Task(name = name, categoryId = categoryId, dueDate = dueDate)
                taskDao.insert(task)

                // If the task has a due date, schedule a reminder
                dueDate?.let {
                    scheduleTaskReminder(task)
                }
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun editTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
            task.dueDate?.let { dueDate ->
                val delay = dueDate - System.currentTimeMillis()
                if (delay > 0) {
                    val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf("taskName" to task.name))
                        .build()
                    WorkManager.getInstance(getApplication()).enqueue(reminderRequest)
                }
            }
        }
    }


    // Schedule reminder for task
    private fun scheduleTaskReminder(task: Task) {
        val delayTime = task.dueDate?.minus(System.currentTimeMillis()) ?: return

        val data = workDataOf(
            "task_title" to task.name,
            "task_id" to task.id
        )

        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayTime, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(reminderRequest)
    }

    // Remove a task
    fun removeTask(task: Task) {
        viewModelScope.launch {
            try {
                taskDao.delete(task)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    // Toggle task completion status
    fun toggleTask(task: Task, isDone: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isDone = isDone)
                taskDao.update(updatedTask)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    // Add a category
    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                val category = Category(name = name)
                categoryDao.insert(category)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    // Get tasks by category ID
    fun getTasksByCategory(categoryId: Long): LiveData<List<Task>> {
        return taskDao.getTasksByCategory(categoryId)
    }
}
