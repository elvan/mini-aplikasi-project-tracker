package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Project

/**
 * Finds every existing project whose [Project.startDate]..[Project.endDate] range overlaps
 * [newProject]'s range (spec section 8 / backlog 21). Validated globally across all projects in
 * the database, not just dependency-related ones (see backlog 21's Catatan Teknis).
 *
 * Two ranges overlap, inclusive of touching endpoints, when `startA <= endB && startB <= endA`.
 *
 * [newProject] is matched against [existingProjects] by id, so when editing an existing project
 * the caller can simply pass the full project list (including the project being edited, with its
 * old dates) — it is excluded from its own conflict check by id equality, without the caller
 * needing to filter it out first.
 */
object FindScheduleConflictsUseCase {

    fun findScheduleConflicts(newProject: Project, existingProjects: List<Project>): List<Project> =
        existingProjects.filter { it.id != newProject.id && overlaps(newProject, it) }

    private fun overlaps(a: Project, b: Project): Boolean =
        !a.startDate.isAfter(b.endDate) && !b.startDate.isAfter(a.endDate)
}
