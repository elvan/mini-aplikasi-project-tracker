package com.example.projecttracker.data.repository

import com.example.projecttracker.data.local.dao.TaskDependencyDao
import com.example.projecttracker.data.local.entity.TaskDependency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskDependencyRepository(private val taskDependencyDao: TaskDependencyDao) {

    suspend fun insert(dependency: TaskDependency) = withContext(Dispatchers.IO) {
        taskDependencyDao.insert(dependency)
    }

    suspend fun delete(dependency: TaskDependency) = withContext(Dispatchers.IO) {
        taskDependencyDao.delete(dependency)
    }

    suspend fun getDependenciesOf(taskId: Long): List<TaskDependency> = withContext(Dispatchers.IO) {
        taskDependencyDao.getDependenciesOf(taskId)
    }

    suspend fun getDependents(taskId: Long): List<TaskDependency> = withContext(Dispatchers.IO) {
        taskDependencyDao.getDependents(taskId)
    }
}
