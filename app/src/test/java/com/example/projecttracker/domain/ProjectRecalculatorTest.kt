package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectRecalculatorTest {

    private fun task(bobot: Int, status: TaskStatus, id: Long = 0) =
        Task(id = id, nama = "Task $id", status = status, projectId = 1, bobot = bobot)

    @Test
    fun `progress matches spesifikasi example - 2 done 1 bobot draft`() {
        val tasks = listOf(
            task(bobot = 2, status = TaskStatus.DONE),
            task(bobot = 1, status = TaskStatus.DRAFT)
        )

        val progress = ProjectRecalculator.calculateProjectProgress(tasks)

        assertEquals(66.6, progress, 0.1)
    }

    @Test
    fun `progress is zero when project has no tasks`() {
        val progress = ProjectRecalculator.calculateProjectProgress(emptyList())

        assertEquals(0.0, progress, 0.0)
    }

    @Test
    fun `progress is zero when total bobot of all tasks is zero`() {
        val tasks = listOf(
            task(bobot = 0, status = TaskStatus.DONE),
            task(bobot = 0, status = TaskStatus.DRAFT)
        )

        val progress = ProjectRecalculator.calculateProjectProgress(tasks)

        assertEquals(0.0, progress, 0.0)
    }

    @Test
    fun `progress is 100 when all tasks are done`() {
        val tasks = listOf(
            task(bobot = 3, status = TaskStatus.DONE),
            task(bobot = 5, status = TaskStatus.DONE)
        )

        val progress = ProjectRecalculator.calculateProjectProgress(tasks)

        assertEquals(100.0, progress, 0.0)
    }

    @Test
    fun `progress is 0 when no task is done`() {
        val tasks = listOf(
            task(bobot = 3, status = TaskStatus.DRAFT),
            task(bobot = 5, status = TaskStatus.IN_PROGRESS)
        )

        val progress = ProjectRecalculator.calculateProjectProgress(tasks)

        assertEquals(0.0, progress, 0.0)
    }

    @Test
    fun `only bobot of Done tasks counts toward progress, In Progress does not`() {
        val tasks = listOf(
            task(bobot = 1, status = TaskStatus.DONE),
            task(bobot = 1, status = TaskStatus.IN_PROGRESS),
            task(bobot = 2, status = TaskStatus.DRAFT)
        )

        val progress = ProjectRecalculator.calculateProjectProgress(tasks)

        assertEquals(25.0, progress, 0.0)
    }

    @Test
    fun `status is Draft when project has no tasks`() {
        val status = ProjectRecalculator.calculateProjectStatus(emptyList())

        assertEquals(ProjectStatus.DRAFT, status)
    }

    @Test
    fun `status is Draft when all tasks are Draft`() {
        val tasks = listOf(
            task(bobot = 1, status = TaskStatus.DRAFT),
            task(bobot = 2, status = TaskStatus.DRAFT)
        )

        val status = ProjectRecalculator.calculateProjectStatus(tasks)

        assertEquals(ProjectStatus.DRAFT, status)
    }

    @Test
    fun `status is Done when all tasks are Done`() {
        val tasks = listOf(
            task(bobot = 1, status = TaskStatus.DONE),
            task(bobot = 2, status = TaskStatus.DONE)
        )

        val status = ProjectRecalculator.calculateProjectStatus(tasks)

        assertEquals(ProjectStatus.DONE, status)
    }

    @Test
    fun `status is In Progress when at least one task is In Progress`() {
        val tasks = listOf(
            task(bobot = 1, status = TaskStatus.DRAFT),
            task(bobot = 2, status = TaskStatus.IN_PROGRESS),
            task(bobot = 3, status = TaskStatus.DONE)
        )

        val status = ProjectRecalculator.calculateProjectStatus(tasks)

        assertEquals(ProjectStatus.IN_PROGRESS, status)
    }

    @Test
    fun `status is In Progress for mixed Draft and Done without any In Progress`() {
        val tasks = listOf(
            task(bobot = 1, status = TaskStatus.DRAFT),
            task(bobot = 2, status = TaskStatus.DONE)
        )

        val status = ProjectRecalculator.calculateProjectStatus(tasks)

        assertEquals(ProjectStatus.IN_PROGRESS, status)
    }

    @Test
    fun `recalculate combines progress and status from tasks when there are no dependencies`() {
        val tasks = listOf(
            task(bobot = 2, status = TaskStatus.DONE),
            task(bobot = 1, status = TaskStatus.DRAFT)
        )

        val result = ProjectRecalculator.recalculate(tasks, dependencyStatuses = emptyList())

        assertEquals(66.6, result.progress, 0.1)
        assertEquals(ProjectStatus.IN_PROGRESS, result.status)
    }

    @Test
    fun `recalculate holds status at Draft when a dependency project is not Done`() {
        val tasks = listOf(task(bobot = 1, status = TaskStatus.DONE))

        val result = ProjectRecalculator.recalculate(
            tasks,
            dependencyStatuses = listOf(ProjectStatus.IN_PROGRESS)
        )

        assertEquals(ProjectStatus.DRAFT, result.status)
    }

    @Test
    fun `recalculate allows status through when all dependency projects are Done`() {
        val tasks = listOf(task(bobot = 1, status = TaskStatus.DONE))

        val result = ProjectRecalculator.recalculate(
            tasks,
            dependencyStatuses = listOf(ProjectStatus.DONE, ProjectStatus.DONE)
        )

        assertEquals(ProjectStatus.DONE, result.status)
    }

    @Test
    fun `recalculate does not hold back Draft status even with unfinished dependencies`() {
        val tasks = listOf(task(bobot = 1, status = TaskStatus.DRAFT))

        val result = ProjectRecalculator.recalculate(
            tasks,
            dependencyStatuses = listOf(ProjectStatus.DRAFT)
        )

        assertEquals(ProjectStatus.DRAFT, result.status)
    }
}
