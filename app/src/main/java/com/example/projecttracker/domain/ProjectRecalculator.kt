package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus

object ProjectRecalculator {

    data class RecalculationResult(
        val progress: Double,
        val status: ProjectStatus
    )

    fun calculateProjectProgress(tasks: List<Task>): Double {
        val totalBobot = tasks.sumOf { it.bobot }
        if (totalBobot == 0) return 0.0
        val doneBobot = tasks.filter { it.status == TaskStatus.DONE }.sumOf { it.bobot }
        return doneBobot.toDouble() / totalBobot.toDouble() * 100.0
    }

    fun calculateProjectStatus(tasks: List<Task>): ProjectStatus {
        if (tasks.isEmpty() || tasks.all { it.status == TaskStatus.DRAFT }) return ProjectStatus.DRAFT
        if (tasks.all { it.status == TaskStatus.DONE }) return ProjectStatus.DONE
        return ProjectStatus.IN_PROGRESS
    }

    // Combines progress + status-from-tasks + the project-dependency rule (spec section 6 /
    // backlog 19) in one call so every recalculation site applies them in the same order.
    // dependencyStatuses only holds this project's own direct dependencies' current statuses;
    // cascading revalidation of *dependents* and circular-dependency rejection are backlog 19's
    // job, not this pure function's.
    fun recalculate(tasks: List<Task>, dependencyStatuses: List<ProjectStatus> = emptyList()): RecalculationResult {
        val progress = calculateProjectProgress(tasks)
        val statusFromTasks = calculateProjectStatus(tasks)
        val status = applyDependencyConstraint(statusFromTasks, dependencyStatuses)
        return RecalculationResult(progress, status)
    }

    // Project tidak boleh In Progress/Done selama ada dependency yang belum Done (spec 6);
    // status ditahan di Draft sampai seluruh dependency selesai.
    private fun applyDependencyConstraint(
        statusFromTasks: ProjectStatus,
        dependencyStatuses: List<ProjectStatus>
    ): ProjectStatus {
        val blockedByDependency = statusFromTasks != ProjectStatus.DRAFT &&
            dependencyStatuses.any { it != ProjectStatus.DONE }
        return if (blockedByDependency) ProjectStatus.DRAFT else statusFromTasks
    }
}
