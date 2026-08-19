package com.example.projecttracker.domain

/**
 * Checks whether adding/editing a project's dependency set would introduce a circular
 * dependency, directly or transitively, across the whole project-dependency graph.
 *
 * Same graph-traversal approach as [DetectCircularDependencyUseCase] (backlog 18), reused
 * here via [TaskDependencyValidator.hasCycle] since both graphs are keyed by Long ids.
 */
object DetectCircularProjectDependencyUseCase {

    fun wouldCreateCycle(
        allProjectIds: Collection<Long>,
        existingDependencyEdges: Map<Long, List<Long>>,
        editedProjectId: Long,
        newDependencyIds: Set<Long>
    ): Boolean {
        val graph = allProjectIds
            .filter { it != editedProjectId }
            .associateWith { existingDependencyEdges[it].orEmpty() }
            .toMutableMap()
        graph[editedProjectId] = newDependencyIds.toList()

        return TaskDependencyValidator.hasCycle(graph)
    }
}
