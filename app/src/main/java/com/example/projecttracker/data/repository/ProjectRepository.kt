package com.example.projecttracker.data.repository

import com.example.projecttracker.data.local.dao.ProjectDao
import com.example.projecttracker.data.local.entity.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ProjectRepository(private val projectDao: ProjectDao) {

    suspend fun insert(project: Project): Long = withContext(Dispatchers.IO) {
        projectDao.insert(project)
    }

    suspend fun update(project: Project) = withContext(Dispatchers.IO) {
        projectDao.update(project)
    }

    suspend fun delete(project: Project) = withContext(Dispatchers.IO) {
        projectDao.delete(project)
    }

    suspend fun getById(id: Long): Project? = withContext(Dispatchers.IO) {
        projectDao.getById(id)
    }

    fun getAll(): Flow<List<Project>> = projectDao.getAll()

    // Sekali ambil (bukan Flow) karena dipakai untuk validasi (circular dependency, dsb), bukan
    // observasi UI.
    suspend fun getAllOnce(): List<Project> = withContext(Dispatchers.IO) {
        projectDao.getAll().first()
    }
}
