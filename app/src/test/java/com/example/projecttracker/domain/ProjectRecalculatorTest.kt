package com.example.projecttracker.domain

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
}
