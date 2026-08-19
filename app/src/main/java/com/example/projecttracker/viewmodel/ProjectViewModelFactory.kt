package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskRepository

class ProjectViewModelFactory(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val projectDependencyRepository: ProjectDependencyRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ProjectViewModel(projectRepository, taskRepository, projectDependencyRepository) as T
    }
}
