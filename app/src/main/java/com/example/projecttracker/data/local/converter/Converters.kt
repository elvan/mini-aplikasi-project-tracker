package com.example.projecttracker.data.local.converter

import androidx.room.TypeConverter
import com.example.projecttracker.data.local.entity.ProjectStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class Converters {

    @TypeConverter
    fun fromEpochMillis(value: Long?): LocalDate? =
        value?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

    @TypeConverter
    fun localDateToEpochMillis(date: LocalDate?): Long? =
        date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun fromProjectStatus(status: ProjectStatus): String = status.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)
}
