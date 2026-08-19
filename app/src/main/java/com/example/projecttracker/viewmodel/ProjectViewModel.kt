package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProjectViewModel(private val projectRepository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun getProjectById(id: Long): Project? = projectRepository.getById(id)

    fun addProject(nama: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            projectRepository.insert(
                Project(
                    nama = nama,
                    status = ProjectStatus.DRAFT,
                    completionProgress = 0.0,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            projectRepository.update(project)
        }
    }
}
