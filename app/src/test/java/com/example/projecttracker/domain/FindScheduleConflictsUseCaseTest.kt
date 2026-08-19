package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FindScheduleConflictsUseCaseTest {

    private fun project(id: Long, start: String, end: String, nama: String = "Project $id") =
        Project(id = id, nama = nama, startDate = LocalDate.parse(start), endDate = LocalDate.parse(end))

    @Test
    fun `no overlap - ranges completely apart - passes`() {
        val newProject = project(id = 0, start = "2026-01-01", end = "2026-01-10")
        val existing = listOf(project(id = 1, start = "2026-02-01", end = "2026-02-10"))

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(newProject, existing)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `full overlap - new range entirely inside existing range - conflicts`() {
        val newProject = project(id = 0, start = "2026-01-05", end = "2026-01-07")
        val other = project(id = 1, start = "2026-01-01", end = "2026-01-31")

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(newProject, listOf(other))

        assertEquals(listOf(other), conflicts)
    }

    @Test
    fun `partial overlap at start - new range starts before and ends inside existing - conflicts`() {
        val newProject = project(id = 0, start = "2026-01-01", end = "2026-01-10")
        val other = project(id = 1, start = "2026-01-05", end = "2026-01-20")

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(newProject, listOf(other))

        assertEquals(listOf(other), conflicts)
    }

    @Test
    fun `partial overlap at end - new range starts inside and ends after existing - conflicts`() {
        val newProject = project(id = 0, start = "2026-01-15", end = "2026-01-31")
        val other = project(id = 1, start = "2026-01-01", end = "2026-01-20")

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(newProject, listOf(other))

        assertEquals(listOf(other), conflicts)
    }

    @Test
    fun `touching endpoints - new start equals existing end - conflicts (inclusive boundary)`() {
        val newProject = project(id = 0, start = "2026-01-10", end = "2026-01-20")
        val other = project(id = 1, start = "2026-01-01", end = "2026-01-10")

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(newProject, listOf(other))

        assertEquals(listOf(other), conflicts)
    }

    @Test
    fun `editing a project does not conflict with itself`() {
        val editedProject = project(id = 1, start = "2026-01-01", end = "2026-01-10", nama = "Edited")
        val allProjects = listOf(
            project(id = 1, start = "2026-01-01", end = "2026-01-10", nama = "Edited"),
            project(id = 2, start = "2026-02-01", end = "2026-02-10")
        )

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(editedProject, allProjects)

        assertTrue(conflicts.isEmpty())
    }

    @Test
    fun `editing a project still detects conflicts with other projects`() {
        val editedProject = project(id = 1, start = "2026-01-05", end = "2026-01-15", nama = "Edited")
        val other = project(id = 2, start = "2026-01-10", end = "2026-01-20")
        val allProjects = listOf(editedProject, other)

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(editedProject, allProjects)

        assertEquals(listOf(other), conflicts)
    }

    @Test
    fun `multiple conflicting projects are all returned`() {
        val newProject = project(id = 0, start = "2026-01-01", end = "2026-01-31")
        val first = project(id = 1, start = "2026-01-05", end = "2026-01-10")
        val second = project(id = 2, start = "2026-01-20", end = "2026-01-25")
        val unrelated = project(id = 3, start = "2026-03-01", end = "2026-03-10")

        val conflicts = FindScheduleConflictsUseCase.findScheduleConflicts(
            newProject,
            listOf(first, second, unrelated)
        )

        assertEquals(listOf(first, second), conflicts)
    }
}
