package com.example.projecttracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["taskId", "dependsOnTaskId"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dependsOnTaskId")
    ]
)
data class TaskDependency(
    val taskId: Long,
    val dependsOnTaskId: Long
)
