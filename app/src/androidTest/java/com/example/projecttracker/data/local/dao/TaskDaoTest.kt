package com.example.projecttracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskDependency
import com.example.projecttracker.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDependencyDao: TaskDependencyDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        taskDao = database.taskDao()
        projectDao = database.projectDao()
        taskDependencyDao = database.taskDependencyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertProject(): Long = projectDao.insert(
        Project(
            nama = "Project Alpha",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 3, 1)
        )
    )

    @Test
    fun insertThenGetAllByProject_returnsInsertedTask() = runBlocking {
        val projectId = insertProject()
        val task = Task(nama = "Task 1", projectId = projectId, bobot = 2)

        taskDao.insert(task)
        val tasks = taskDao.getAllByProject(projectId).first()

        assertEquals(1, tasks.size)
        assertEquals("Task 1", tasks[0].nama)
        assertEquals(2, tasks[0].bobot)
    }

    @Test
    fun update_changesPersistedTask() = runBlocking {
        val projectId = insertProject()
        val taskId = taskDao.insert(Task(nama = "Task 1", projectId = projectId, bobot = 2))

        val stored = taskDao.getById(taskId)!!
        taskDao.update(stored.copy(status = TaskStatus.DONE))

        assertEquals(TaskStatus.DONE, taskDao.getById(taskId)!!.status)
    }

    @Test
    fun delete_removesTask() = runBlocking {
        val projectId = insertProject()
        val taskId = taskDao.insert(Task(nama = "Task 1", projectId = projectId, bobot = 2))

        taskDao.delete(taskDao.getById(taskId)!!)

        assertNull(taskDao.getById(taskId))
    }

    @Test
    fun getSubtasks_returnsOnlyChildrenOfParent() = runBlocking {
        val projectId = insertProject()
        val parentId = taskDao.insert(Task(nama = "Parent", projectId = projectId, bobot = 3))
        taskDao.insert(Task(nama = "Subtask 1", projectId = projectId, bobot = 1, parentTaskId = parentId))
        taskDao.insert(Task(nama = "Subtask 2", projectId = projectId, bobot = 1, parentTaskId = parentId))
        taskDao.insert(Task(nama = "Unrelated", projectId = projectId, bobot = 1))

        val subtasks = taskDao.getSubtasks(parentId).first()

        assertEquals(2, subtasks.size)
        assertEquals(setOf("Subtask 1", "Subtask 2"), subtasks.map { it.nama }.toSet())
    }

    @Test
    fun delete_cascadesToSubtasksTransitively() = runBlocking {
        val projectId = insertProject()
        val parentId = taskDao.insert(Task(nama = "Parent", projectId = projectId, bobot = 3))
        val childId = taskDao.insert(Task(nama = "Child", projectId = projectId, bobot = 1, parentTaskId = parentId))
        val grandchildId = taskDao.insert(Task(nama = "Grandchild", projectId = projectId, bobot = 1, parentTaskId = childId))

        taskDao.delete(taskDao.getById(parentId)!!)

        assertNull(taskDao.getById(parentId))
        assertNull(taskDao.getById(childId))
        assertNull(taskDao.getById(grandchildId))
    }

    @Test
    fun delete_cascadesToOwnDependencies() = runBlocking {
        val projectId = insertProject()
        val taskId = taskDao.insert(Task(nama = "Task 1", projectId = projectId, bobot = 2))
        val dependsOnId = taskDao.insert(Task(nama = "Task 2", projectId = projectId, bobot = 2))
        taskDependencyDao.insert(TaskDependency(taskId = taskId, dependsOnTaskId = dependsOnId))

        taskDao.delete(taskDao.getById(taskId)!!)

        assertTrue(taskDependencyDao.getDependenciesOf(taskId).isEmpty())
        assertNull(taskDao.getById(taskId))
        assertEquals("Task 2", taskDao.getById(dependsOnId)?.nama)
    }

    @Test
    fun delete_cascadesToDependenciesOfOtherTasks() = runBlocking {
        val projectId = insertProject()
        val dependentId = taskDao.insert(Task(nama = "Dependent", projectId = projectId, bobot = 2))
        val dependencyId = taskDao.insert(Task(nama = "Dependency", projectId = projectId, bobot = 2))
        taskDependencyDao.insert(TaskDependency(taskId = dependentId, dependsOnTaskId = dependencyId))

        taskDao.delete(taskDao.getById(dependencyId)!!)

        assertNull(taskDao.getById(dependencyId))
        assertTrue(taskDependencyDao.getDependenciesOf(dependentId).isEmpty())
        assertEquals("Dependent", taskDao.getById(dependentId)?.nama)
    }
}
