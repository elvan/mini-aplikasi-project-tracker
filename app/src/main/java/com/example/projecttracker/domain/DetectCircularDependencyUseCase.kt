package com.example.projecttracker.domain

/**
 * Checks whether adding/editing a task's dependency set would introduce a circular
 * dependency, directly or transitively, across the whole dependency graph.
 */
object DetectCircularDependencyUseCase {

    fun wouldCreateCycle(
        allTaskIds: Collection<Long>,
        existingDependencyEdges: Map<Long, List<Long>>,
        editedTaskId: Long,
        newDependencyIds: Set<Long>
    ): Boolean {
        val graph = allTaskIds
            .filter { it != editedTaskId }
            .associateWith { existingDependencyEdges[it].orEmpty() }
            .toMutableMap()
        graph[editedTaskId] = newDependencyIds.toList()

        return TaskDependencyValidator.hasCycle(graph)
    }
}
