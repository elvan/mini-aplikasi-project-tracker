package com.example.projecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val completionProgress: Double = 0.0,
    val startDate: LocalDate,
    val endDate: LocalDate
)
