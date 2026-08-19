package com.example.projecttracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.local.entity.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProjectDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var projectDao: ProjectDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        projectDao = database.projectDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertThenGetAll_returnsInsertedProject() = runBlocking {
        val project = Project(
            nama = "Project Alpha",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 3, 1)
        )

        projectDao.insert(project)
        val projects = projectDao.getAll().first()

        assertEquals(1, projects.size)
        assertEquals("Project Alpha", projects[0].nama)
    }
}
