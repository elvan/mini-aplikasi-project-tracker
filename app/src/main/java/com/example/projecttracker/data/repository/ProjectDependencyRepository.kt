package com.example.projecttracker.data.repository

import com.example.projecttracker.data.local.dao.ProjectDependencyDao
import com.example.projecttracker.data.local.entity.ProjectDependency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectDependencyRepository(private val projectDependencyDao: ProjectDependencyDao) {

    suspend fun insert(dependency: ProjectDependency) = withContext(Dispatchers.IO) {
        projectDependencyDao.insert(dependency)
    }

    suspend fun delete(dependency: ProjectDependency) = withContext(Dispatchers.IO) {
        projectDependencyDao.delete(dependency)
    }

    suspend fun getDependenciesOf(projectId: Long): List<ProjectDependency> = withContext(Dispatchers.IO) {
        projectDependencyDao.getDependenciesOf(projectId)
    }

    suspend fun getDependents(projectId: Long): List<ProjectDependency> = withContext(Dispatchers.IO) {
        projectDependencyDao.getDependents(projectId)
    }
}
