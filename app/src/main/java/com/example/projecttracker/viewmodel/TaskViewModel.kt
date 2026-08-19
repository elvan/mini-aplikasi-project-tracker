package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TaskListItem(val task: Task, val level: Int)

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val projectId: Long
) : ViewModel() {

    val tasks: StateFlow<List<TaskListItem>> = taskRepository.getAllByProject(projectId)
        .map { tasks -> flattenHierarchy(tasks) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private fun flattenHierarchy(tasks: List<Task>): List<TaskListItem> {
        val childrenByParent = tasks.groupBy { it.parentTaskId }
        val result = mutableListOf<TaskListItem>()

        fun addChildren(parentId: Long?, level: Int) {
            for (task in childrenByParent[parentId].orEmpty()) {
                result += TaskListItem(task, level)
                addChildren(task.id, level + 1)
            }
        }

        addChildren(null, 0)
        return result
    }
}
