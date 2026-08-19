package com.example.projecttracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.ProjectDependency
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.domain.DetectCircularProjectDependencyUseCase
import com.example.projecttracker.domain.RecalculateProjectUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class ProjectSaveError {
    object CircularDependency : ProjectSaveError()
}

sealed class ProjectSaveResult {
    object Success : ProjectSaveResult()
    data class Error(val error: ProjectSaveError) : ProjectSaveResult()
}

class ProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val projectDependencyRepository: ProjectDependencyRepository
) : ViewModel() {

    private val recalculateProjectUseCase =
        RecalculateProjectUseCase(projectRepository, taskRepository, projectDependencyRepository)

    val projects: StateFlow<List<Project>> = projectRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    suspend fun getProjectById(id: Long): Project? = projectRepository.getById(id)

    suspend fun getAllProjectsOnce(): List<Project> = projectRepository.getAllOnce()

    suspend fun getDependencyIdsOf(projectId: Long): Set<Long> =
        projectDependencyRepository.getDependenciesOf(projectId).map { it.dependsOnProjectId }.toSet()

    suspend fun saveProject(
        projectId: Long?,
        nama: String,
        startDate: LocalDate,
        endDate: LocalDate,
        dependencyIds: Set<Long>
    ): ProjectSaveResult {
        val allProjects = projectRepository.getAllOnce()

        val existingDependencyEdges = allProjects
            .filter { it.id != projectId }
            .associate { it.id to projectDependencyRepository.getDependenciesOf(it.id).map { dep -> dep.dependsOnProjectId } }

        val wouldCycle = DetectCircularProjectDependencyUseCase.wouldCreateCycle(
            allProjectIds = allProjects.map { it.id },
            existingDependencyEdges = existingDependencyEdges,
            editedProjectId = projectId ?: NEW_PROJECT_NODE_ID,
            newDependencyIds = dependencyIds
        )
        if (wouldCycle) {
            return ProjectSaveResult.Error(ProjectSaveError.CircularDependency)
        }

        val savedProjectId = if (projectId == null) {
            projectRepository.insert(Project(nama = nama, startDate = startDate, endDate = endDate))
        } else {
            // Preserve the derived status/completionProgress here; recalculateProjectUseCase
            // below is what's allowed to change them.
            val existing = projectRepository.getById(projectId) ?: return ProjectSaveResult.Success
            projectRepository.update(existing.copy(nama = nama, startDate = startDate, endDate = endDate))
            projectId
        }

        val existingDependencyIds = if (projectId != null) getDependencyIdsOf(projectId) else emptySet()
        (dependencyIds - existingDependencyIds).forEach {
            projectDependencyRepository.insert(ProjectDependency(savedProjectId, it))
        }
        (existingDependencyIds - dependencyIds).forEach {
            projectDependencyRepository.delete(ProjectDependency(savedProjectId, it))
        }

        recalculateProjectUseCase(savedProjectId)

        return ProjectSaveResult.Success
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            // Dependency rows pointing at/from this project cascade-delete with it (FK
            // ON DELETE CASCADE); capture former dependents first so they can be re-validated
            // afterwards — a project that was held back by this dependency may now proceed.
            val formerDependents = projectDependencyRepository.getDependents(projectId).map { it.projectId }
            projectRepository.delete(project)
            formerDependents.forEach { recalculateProjectUseCase(it) }
        }
    }

    private companion object {
        // No real project can have this id (Room autoIncrement starts at 1); used as the
        // new-project's graph node when checking for cycles before an insert has produced a
        // real id.
        const val NEW_PROJECT_NODE_ID = 0L
    }
}
