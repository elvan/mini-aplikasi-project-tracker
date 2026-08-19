package com.example.projecttracker.domain

import com.example.projecttracker.data.local.dao.ProjectDao
import com.example.projecttracker.data.local.dao.ProjectDependencyDao
import com.example.projecttracker.data.local.dao.TaskDao
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.ProjectDependency
import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** In-memory [ProjectDao] fake so [RecalculateProjectUseCase] can be unit tested without Room. */
private class FakeProjectDao : ProjectDao {
    val projects = mutableMapOf<Long, Project>()
    private var nextId = 1L

    override suspend fun insert(project: Project): Long {
        val id = nextId++
        projects[id] = project.copy(id = id)
        return id
    }

    override suspend fun update(project: Project) {
        projects[project.id] = project
    }

    override suspend fun delete(project: Project) {
        projects.remove(project.id)
    }

    override suspend fun getById(id: Long): Project? = projects[id]

    override fun getAll(): Flow<List<Project>> = flowOf(projects.values.toList())
}

private class FakeTaskDao : TaskDao {
    val tasks = mutableMapOf<Long, Task>()
    private var nextId = 1L

    override suspend fun insert(task: Task): Long {
        val id = nextId++
        tasks[id] = task.copy(id = id)
        return id
    }

    override suspend fun update(task: Task) {
        tasks[task.id] = task
    }

    override suspend fun delete(task: Task) {
        tasks.remove(task.id)
    }

    override suspend fun getById(id: Long): Task? = tasks[id]

    override fun getAllByProject(projectId: Long): Flow<List<Task>> =
        flowOf(tasks.values.filter { it.projectId == projectId })

    override fun getSubtasks(parentTaskId: Long): Flow<List<Task>> =
        flowOf(tasks.values.filter { it.parentTaskId == parentTaskId })
}

private class FakeProjectDependencyDao : ProjectDependencyDao {
    val edges = mutableListOf<ProjectDependency>()

    override suspend fun insertRaw(dependency: ProjectDependency) {
        edges += dependency
    }

    override suspend fun delete(dependency: ProjectDependency) {
        edges.remove(dependency)
    }

    override suspend fun getDependenciesOf(projectId: Long): List<ProjectDependency> =
        edges.filter { it.projectId == projectId }

    override suspend fun getDependents(projectId: Long): List<ProjectDependency> =
        edges.filter { it.dependsOnProjectId == projectId }
}

class RecalculateProjectUseCaseTest {

    private val projectDao = FakeProjectDao()
    private val taskDao = FakeTaskDao()
    private val dependencyDao = FakeProjectDependencyDao()

    private val projectRepository = ProjectRepository(projectDao)
    private val taskRepository = TaskRepository(taskDao)
    private val dependencyRepository = ProjectDependencyRepository(dependencyDao)

    private val useCase = RecalculateProjectUseCase(projectRepository, taskRepository, dependencyRepository)

    private val today = LocalDate.of(2026, 1, 1)

    private fun addProject(id: Long, status: ProjectStatus = ProjectStatus.DRAFT) {
        projectDao.projects[id] = Project(
            id = id,
            nama = "Project $id",
            status = status,
            completionProgress = 0.0,
            startDate = today,
            endDate = today.plusDays(10)
        )
    }

    private fun addTask(id: Long, projectId: Long, status: TaskStatus, bobot: Int = 1) {
        taskDao.tasks[id] = Task(id = id, nama = "Task $id", status = status, projectId = projectId, bobot = bobot)
    }

    @Test
    fun `project is held at Draft when a dependency project is not Done`() = runBlocking {
        addProject(1) // depends on 2
        addProject(2)
        addTask(id = 1, projectId = 1, status = TaskStatus.DONE)
        dependencyDao.edges += ProjectDependency(projectId = 1, dependsOnProjectId = 2)

        useCase(1)

        assertEquals(ProjectStatus.DRAFT, projectDao.projects.getValue(1).status)
    }

    @Test
    fun `project proceeds when all dependency projects are Done`() = runBlocking {
        addProject(1) // depends on 2
        addProject(2, status = ProjectStatus.DONE)
        addTask(id = 1, projectId = 1, status = TaskStatus.DONE)
        dependencyDao.edges += ProjectDependency(projectId = 1, dependsOnProjectId = 2)

        useCase(1)

        assertEquals(ProjectStatus.DONE, projectDao.projects.getValue(1).status)
    }

    @Test
    fun `cascading - dependency dropping from Done holds its dependent back too`() = runBlocking {
        // Project 2 depends on nothing and starts fully Done; project 1 depends on 2 and is
        // also fully Done as long as 2 stays Done.
        addProject(1)
        addProject(2)
        addTask(id = 1, projectId = 1, status = TaskStatus.DONE)
        addTask(id = 2, projectId = 2, status = TaskStatus.DONE)
        dependencyDao.edges += ProjectDependency(projectId = 1, dependsOnProjectId = 2)

        useCase(2)
        useCase(1)
        assertEquals(ProjectStatus.DONE, projectDao.projects.getValue(1).status)
        assertEquals(ProjectStatus.DONE, projectDao.projects.getValue(2).status)

        // Project 2 regresses: a new Draft task is added, dropping its status from Done.
        addTask(id = 3, projectId = 2, status = TaskStatus.DRAFT)

        useCase(2)

        assertEquals(ProjectStatus.IN_PROGRESS, projectDao.projects.getValue(2).status)
        // Cascaded: project 1 must be re-validated and held back, even though its own tasks
        // are still all Done and nobody called useCase(1) directly.
        assertEquals(ProjectStatus.DRAFT, projectDao.projects.getValue(1).status)
    }

    @Test
    fun `cascading propagates through a multi-level dependency chain`() = runBlocking {
        // 1 depends on 2, 2 depends on 3. All start Done.
        addProject(1)
        addProject(2)
        addProject(3)
        addTask(id = 1, projectId = 1, status = TaskStatus.DONE)
        addTask(id = 2, projectId = 2, status = TaskStatus.DONE)
        addTask(id = 3, projectId = 3, status = TaskStatus.DONE)
        dependencyDao.edges += ProjectDependency(projectId = 1, dependsOnProjectId = 2)
        dependencyDao.edges += ProjectDependency(projectId = 2, dependsOnProjectId = 3)

        useCase(3)
        useCase(2)
        useCase(1)
        assertEquals(ProjectStatus.DONE, projectDao.projects.getValue(1).status)

        // Project 3 regresses; this should cascade through 2 and all the way to 1.
        addTask(id = 4, projectId = 3, status = TaskStatus.DRAFT)

        useCase(3)

        assertEquals(ProjectStatus.IN_PROGRESS, projectDao.projects.getValue(3).status)
        assertEquals(ProjectStatus.DRAFT, projectDao.projects.getValue(2).status)
        assertEquals(ProjectStatus.DRAFT, projectDao.projects.getValue(1).status)
    }
}
