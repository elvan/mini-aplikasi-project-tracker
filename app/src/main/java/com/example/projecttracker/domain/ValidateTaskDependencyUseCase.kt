package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus

/**
 * Enforces spec section 5: a task can't become Done while any of its dependencies isn't
 * Done, and dependent tasks must be re-checked when a dependency's status changes.
 *
 * Design decision (backlog 18, point 4): when a dependency's status moves away from Done,
 * a dependent task that was already saved as Done is NOT auto-downgraded in the database —
 * the stored status is left as-is. It simply won't pass validation again if the user tries
 * to re-save it as Done. [findInvalidatedDoneDependents] exists so callers can surface a
 * warning about this instead of silently doing nothing.
 */
object ValidateTaskDependencyUseCase {

    fun findBlockingDependency(
        newStatus: TaskStatus,
        dependencyIds: Set<Long>,
        tasksById: Map<Long, Task>
    ): Task? {
        if (newStatus != TaskStatus.DONE) return null
        return dependencyIds.mapNotNull { tasksById[it] }.firstOrNull { it.status != TaskStatus.DONE }
    }

    fun findInvalidatedDoneDependents(
        newStatus: TaskStatus,
        dependentTaskIds: Collection<Long>,
        tasksById: Map<Long, Task>
    ): List<Task> {
        if (newStatus == TaskStatus.DONE) return emptyList()
        return dependentTaskIds.mapNotNull { tasksById[it] }.filter { it.status == TaskStatus.DONE }
    }
}
