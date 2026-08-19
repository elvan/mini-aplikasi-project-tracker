package com.example.projecttracker.data.repository

import com.example.projecttracker.data.local.dao.TaskDao
import com.example.projecttracker.data.local.entity.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    suspend fun insert(task: Task): Long = withContext(Dispatchers.IO) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) = withContext(Dispatchers.IO) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) = withContext(Dispatchers.IO) {
        taskDao.delete(task)
    }

    suspend fun getById(id: Long): Task? = withContext(Dispatchers.IO) {
        taskDao.getById(id)
    }

    fun getAllByProject(projectId: Long): Flow<List<Task>> = taskDao.getAllByProject(projectId)

    fun getSubtasks(parentTaskId: Long): Flow<List<Task>> = taskDao.getSubtasks(parentTaskId)

    // Sekali ambil (bukan Flow) karena dipakai untuk kalkulasi progress, bukan observasi UI.
    suspend fun getTasksForProgressCalculation(projectId: Long): List<Task> =
        withContext(Dispatchers.IO) {
            taskDao.getAllByProject(projectId).first()
        }
}
