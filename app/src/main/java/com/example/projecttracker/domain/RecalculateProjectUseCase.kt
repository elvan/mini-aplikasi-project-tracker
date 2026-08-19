package com.example.projecttracker.domain

import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskRepository

/**
 * Single entrypoint for recalculating a project's progress ([16]) and status ([17]), while
 * also enforcing the project-dependency rule (spec section 6 / backlog 19): a project's status
 * is held at Draft while any of its own dependency projects isn't Done yet.
 *
 * When recalculating changes this project's status, every project that depends on it is
 * re-validated the same way, recursively — this is how a dependency dropping out of Done (or
 * catching up to Done) propagates to everything downstream of it.
 */
class RecalculateProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val projectDependencyRepository: ProjectDependencyRepository
) {

    suspend operator fun invoke(projectId: Long) {
        recalculate(projectId, mutableSetOf())
    }

    // `processed` guards against reprocessing the same project twice within one cascade run.
    // The dependency graph is already guaranteed acyclic (DetectCircularProjectDependencyUseCase
    // rejects any relation that would close a cycle), so this is a safety net against infinite
    // recursion rather than something that should ever actually trigger.
    private suspend fun recalculate(projectId: Long, processed: MutableSet<Long>) {
        if (!processed.add(projectId)) return

        val project = projectRepository.getById(projectId) ?: return
        val tasks = taskRepository.getTasksForProgressCalculation(projectId)
        val dependencyStatuses = projectDependencyRepository.getDependenciesOf(projectId)
            .mapNotNull { projectRepository.getById(it.dependsOnProjectId)?.status }
        val result = ProjectRecalculator.recalculate(tasks, dependencyStatuses)

        val statusChanged = project.status != result.status
        if (project.completionProgress != result.progress || statusChanged) {
            projectRepository.update(project.copy(completionProgress = result.progress, status = result.status))
        }

        if (statusChanged) {
            val dependents = projectDependencyRepository.getDependents(projectId).map { it.projectId }
            dependents.forEach { recalculate(it, processed) }
        }
    }
}
