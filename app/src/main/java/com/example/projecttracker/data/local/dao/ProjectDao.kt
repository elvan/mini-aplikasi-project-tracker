package com.example.projecttracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.projecttracker.data.local.entity.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("SELECT * FROM projects ORDER BY id ASC")
    fun getAll(): Flow<List<Project>>
}
