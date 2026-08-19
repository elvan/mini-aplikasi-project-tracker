package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskDependencyRepository
import com.example.projecttracker.data.repository.TaskRepository

class TaskViewModelFactory(
    private val taskRepository: TaskRepository,
    private val taskDependencyRepository: TaskDependencyRepository,
    private val projectRepository: ProjectRepository,
    private val projectId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return TaskViewModel(taskRepository, taskDependencyRepository, projectRepository, projectId) as T
    }
}
