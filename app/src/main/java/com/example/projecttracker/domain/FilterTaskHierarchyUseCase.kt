package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus

/**
 * Pure filtering logic for backlog 20 (spec section 7): filters a flat task list by status
 * and/or name search while keeping the hierarchy intact. A task matches when it satisfies both
 * the status filter (if any) and the search query (if any); a matching subtask pulls its whole
 * ancestor chain along so a later hierarchy-flattening step never shows a subtask with its
 * parent missing.
 */
object FilterTaskHierarchyUseCase {

    fun filter(tasks: List<Task>, statusFilter: TaskStatus?, query: String): List<Task> {
        val trimmedQuery = query.trim()
        val tasksById = tasks.associateBy { it.id }

        fun matches(task: Task): Boolean {
            val statusMatches = statusFilter == null || task.status == statusFilter
            val queryMatches = trimmedQuery.isEmpty() ||
                task.nama.contains(trimmedQuery, ignoreCase = true)
            return statusMatches && queryMatches
        }

        val keptIds = mutableSetOf<Long>()
        for (task in tasks) {
            if (!matches(task)) continue
            var current: Task? = task
            while (current != null) {
                keptIds += current.id
                current = current.parentTaskId?.let { tasksById[it] }
            }
        }

        return tasks.filter { it.id in keptIds }
    }
}
