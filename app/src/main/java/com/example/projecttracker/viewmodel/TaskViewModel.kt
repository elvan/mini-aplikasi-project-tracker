package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskDependency
import com.example.projecttracker.data.local.entity.TaskStatus
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskDependencyRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.domain.ProjectRecalculator
import com.example.projecttracker.domain.TaskDependencyValidator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListItem(val task: Task, val level: Int)

sealed class TaskSaveError {
    data class DependencyNotDone(val dependencyName: String) : TaskSaveError()
    object CircularDependency : TaskSaveError()
}

sealed class TaskSaveResult {
    object Success : TaskSaveResult()
    data class Error(val error: TaskSaveError) : TaskSaveResult()
}

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val taskDependencyRepository: TaskDependencyRepository,
    private val projectRepository: ProjectRepository,
    private val projectDependencyRepository: ProjectDependencyRepository,
    private val projectId: Long
) : ViewModel() {

    val tasks: StateFlow<List<TaskListItem>> = taskRepository.getAllByProject(projectId)
        .map { tasks -> flattenHierarchy(tasks) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun getTaskById(taskId: Long): Task? = taskRepository.getById(taskId)

    suspend fun getAllTasksInProjectOnce(): List<Task> =
        taskRepository.getTasksForProgressCalculation(projectId)

    suspend fun getDependencyIdsOf(taskId: Long): Set<Long> =
        taskDependencyRepository.getDependenciesOf(taskId).map { it.dependsOnTaskId }.toSet()

    suspend fun saveTask(
        taskId: Long?,
        nama: String,
        status: TaskStatus,
        bobot: Int,
        parentTaskId: Long?,
        dependencyIds: Set<Long>
    ): TaskSaveResult {
        val allTasks = taskRepository.getTasksForProgressCalculation(projectId)
        val tasksById = allTasks.associateBy { it.id }

        if (status == TaskStatus.DONE) {
            val blockingDependency = dependencyIds
                .mapNotNull { tasksById[it] }
                .firstOrNull { it.status != TaskStatus.DONE }
            if (blockingDependency != null) {
                return TaskSaveResult.Error(TaskSaveError.DependencyNotDone(blockingDependency.nama))
            }
        }

        val graph = mutableMapOf<Long, List<Long>>()
        for (task in allTasks) {
            if (task.id == taskId) continue
            graph[task.id] = taskDependencyRepository.getDependenciesOf(task.id).map { it.dependsOnTaskId }
        }
        graph[taskId ?: NEW_TASK_NODE_ID] = dependencyIds.toList()

        if (TaskDependencyValidator.hasCycle(graph)) {
            return TaskSaveResult.Error(TaskSaveError.CircularDependency)
        }

        val savedTaskId = if (taskId == null) {
            taskRepository.insert(
                Task(nama = nama, status = status, projectId = projectId, bobot = bobot, parentTaskId = parentTaskId)
            )
        } else {
            taskRepository.update(
                Task(id = taskId, nama = nama, status = status, projectId = projectId, bobot = bobot, parentTaskId = parentTaskId)
            )
            taskId
        }

        val existingDependencyIds = if (taskId != null) getDependencyIdsOf(taskId) else emptySet()
        (dependencyIds - existingDependencyIds).forEach {
            taskDependencyRepository.insert(TaskDependency(savedTaskId, it))
        }
        (existingDependencyIds - dependencyIds).forEach {
            taskDependencyRepository.delete(TaskDependency(savedTaskId, it))
        }

        recalculateProject()

        return TaskSaveResult.Success
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            // Cascades to subtasks and dependency rows via ON DELETE CASCADE foreign keys.
            val task = taskRepository.getById(taskId) ?: return@launch
            taskRepository.delete(task)
            recalculateProject()
        }
    }

    private suspend fun recalculateProject() {
        val project = projectRepository.getById(projectId) ?: return
        val projectTasks = taskRepository.getTasksForProgressCalculation(projectId)
        val dependencyStatuses = projectDependencyRepository.getDependenciesOf(projectId)
            .mapNotNull { projectRepository.getById(it.dependsOnProjectId)?.status }
        val result = ProjectRecalculator.recalculate(projectTasks, dependencyStatuses)
        if (project.completionProgress != result.progress || project.status != result.status) {
            projectRepository.update(project.copy(completionProgress = result.progress, status = result.status))
        }
    }

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

    private companion object {
        // No real task can have this id (Room autoIncrement starts at 1); used as the new-task's
        // graph node when checking for cycles before an insert has produced a real id.
        const val NEW_TASK_NODE_ID = 0L
    }
}
