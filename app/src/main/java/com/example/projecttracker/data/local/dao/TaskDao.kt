package com.example.projecttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.projecttracker.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY id ASC")
    fun getAllByProject(projectId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentTaskId ORDER BY id ASC")
    fun getSubtasks(parentTaskId: Long): Flow<List<Task>>
}
