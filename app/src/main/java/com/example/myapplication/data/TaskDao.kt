package com.example.myapplication.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.myapplication.model.Task

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Update
    suspend fun update(task: Task)

    // Query to get all tasks, now with categoryId information
    @Query("SELECT * FROM task_table")
    fun getAllTasks(): LiveData<List<Task>>

    // Query to get tasks by category ID
    @Query("SELECT * FROM task_table WHERE categoryId = :categoryId")
    fun getTasksByCategory(categoryId: Long): LiveData<List<Task>>

    // Query to get tasks that are not yet done
    @Query("SELECT * FROM task_table WHERE isDone = 0")
    fun getPendingTasks(): LiveData<List<Task>>

    // Query to get tasks that are due soon or overdue
    @Query("SELECT * FROM task_table WHERE dueDate <= :currentTime AND isDone = 0")
    fun getDueTasks(currentTime: Long): LiveData<List<Task>>

    // Query to update task status
    @Query("UPDATE task_table SET isDone = :isDone WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, isDone: Boolean)
}

