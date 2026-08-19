package com.example.projecttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.projecttracker.data.local.entity.ProjectDependency

@Dao
interface ProjectDependencyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRaw(dependency: ProjectDependency)

    @Delete
    suspend fun delete(dependency: ProjectDependency)

    @Query("SELECT * FROM project_dependencies WHERE projectId = :projectId")
    suspend fun getDependenciesOf(projectId: Long): List<ProjectDependency>

    @Query("SELECT * FROM project_dependencies WHERE dependsOnProjectId = :projectId")
    suspend fun getDependents(projectId: Long): List<ProjectDependency>

    suspend fun insert(dependency: ProjectDependency) {
        require(dependency.projectId != dependency.dependsOnProjectId) {
            "Project tidak boleh depend ke dirinya sendiri (projectId=${dependency.projectId})"
        }
        insertRaw(dependency)
    }
}
