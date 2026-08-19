package com.example.projecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "project_dependencies",
    primaryKeys = ["projectId", "dependsOnProjectId"],
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnProjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dependsOnProjectId")
    ]
)
data class ProjectDependency(
    val projectId: Long,
    val dependsOnProjectId: Long
)
