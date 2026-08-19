package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTaskDependencyUseCaseTest {

    private fun task(id: Long, status: TaskStatus, nama: String = "Task $id") =
        Task(id = id, nama = nama, status = status, projectId = 1, bobot = 1)

    @Test
    fun `moving to Done is rejected when a dependency is not Done`() {
        val dependency = task(id = 1, status = TaskStatus.IN_PROGRESS, nama = "Dependency A")
        val tasksById = mapOf(dependency.id to dependency)

        val blocking = ValidateTaskDependencyUseCase.findBlockingDependency(
            newStatus = TaskStatus.DONE,
            dependencyIds = setOf(dependency.id),
            tasksById = tasksById
        )

        assertEquals("Dependency A", blocking?.nama)
    }

    @Test
    fun `moving to Done is accepted when all dependencies are Done`() {
        val dependencies = listOf(
            task(id = 1, status = TaskStatus.DONE),
            task(id = 2, status = TaskStatus.DONE)
        )
        val tasksById = dependencies.associateBy { it.id }

        val blocking = ValidateTaskDependencyUseCase.findBlockingDependency(
            newStatus = TaskStatus.DONE,
            dependencyIds = tasksById.keys,
            tasksById = tasksById
        )

        assertNull(blocking)
    }

    @Test
    fun `non-Done status transitions are never blocked by dependencies`() {
        val dependency = task(id = 1, status = TaskStatus.DRAFT)
        val tasksById = mapOf(dependency.id to dependency)

        val blocking = ValidateTaskDependencyUseCase.findBlockingDependency(
            newStatus = TaskStatus.IN_PROGRESS,
            dependencyIds = setOf(dependency.id),
            tasksById = tasksById
        )

        assertNull(blocking)
    }

    @Test
    fun `dependents that were Done are flagged when this task leaves Done`() {
        val dependentA = task(id = 10, status = TaskStatus.DONE, nama = "Dependent A")
        val dependentB = task(id = 11, status = TaskStatus.IN_PROGRESS, nama = "Dependent B")
        val tasksById = mapOf(dependentA.id to dependentA, dependentB.id to dependentB)

        val invalidated = ValidateTaskDependencyUseCase.findInvalidatedDoneDependents(
            newStatus = TaskStatus.IN_PROGRESS,
            dependentTaskIds = tasksById.keys,
            tasksById = tasksById
        )

        assertEquals(listOf("Dependent A"), invalidated.map { it.nama })
    }

    @Test
    fun `no dependents are flagged when this task moves to Done`() {
        val dependentA = task(id = 10, status = TaskStatus.DONE)

        val invalidated = ValidateTaskDependencyUseCase.findInvalidatedDoneDependents(
            newStatus = TaskStatus.DONE,
            dependentTaskIds = listOf(dependentA.id),
            tasksById = mapOf(dependentA.id to dependentA)
        )

        assertTrue(invalidated.isEmpty())
    }
}
