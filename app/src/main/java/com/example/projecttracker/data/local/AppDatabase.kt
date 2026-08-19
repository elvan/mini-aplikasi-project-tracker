package com.example.projecttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.projecttracker.data.local.converter.Converters
import com.example.projecttracker.data.local.dao.TaskDependencyDao
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskDependency

@Database(
    entities = [Project::class, Task::class, TaskDependency::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDependencyDao(): TaskDependencyDao

    companion object {
        private const val DATABASE_NAME = "project_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}
