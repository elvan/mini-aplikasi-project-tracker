package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus

object ProjectRecalculator {

    fun calculateProjectProgress(tasks: List<Task>): Double {
        val totalBobot = tasks.sumOf { it.bobot }
        if (totalBobot == 0) return 0.0
        val doneBobot = tasks.filter { it.status == TaskStatus.DONE }.sumOf { it.bobot }
        return doneBobot.toDouble() / totalBobot.toDouble() * 100.0
    }

    fun calculateProjectStatus(tasks: List<Task>): ProjectStatus {
        if (tasks.isEmpty() || tasks.all { it.status == TaskStatus.DRAFT }) return ProjectStatus.DRAFT
        if (tasks.all { it.status == TaskStatus.DONE }) return ProjectStatus.DONE
        return ProjectStatus.IN_PROGRESS
    }
}
