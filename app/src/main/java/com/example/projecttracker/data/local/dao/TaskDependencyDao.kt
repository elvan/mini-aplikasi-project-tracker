package com.example.projecttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.projecttracker.data.local.entity.TaskDependency

@Dao
interface TaskDependencyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRaw(dependency: TaskDependency)

    @Delete
    suspend fun delete(dependency: TaskDependency)

    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    suspend fun getDependenciesOf(taskId: Long): List<TaskDependency>

    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
    suspend fun getDependents(taskId: Long): List<TaskDependency>

    suspend fun insert(dependency: TaskDependency) {
        require(dependency.taskId != dependency.dependsOnTaskId) {
            "Task tidak boleh depend ke dirinya sendiri (taskId=${dependency.taskId})"
        }
        insertRaw(dependency)
    }
}
