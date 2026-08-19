package com.example.projecttracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDependencyValidatorTest {

    @Test
    fun `no cycle when graph is a simple chain`() {
        val graph = mapOf(
            1L to listOf(2L),
            2L to listOf(3L),
            3L to emptyList()
        )

        assertFalse(TaskDependencyValidator.hasCycle(graph))
    }

    @Test
    fun `direct circular dependency is detected`() {
        val graph = mapOf(
            1L to listOf(2L),
            2L to listOf(1L)
        )

        assertTrue(TaskDependencyValidator.hasCycle(graph))
    }

    @Test
    fun `indirect circular dependency across 3 nodes is detected`() {
        val graph = mapOf(
            1L to listOf(2L),
            2L to listOf(3L),
            3L to listOf(1L)
        )

        assertTrue(TaskDependencyValidator.hasCycle(graph))
    }

    @Test
    fun `disconnected acyclic graphs do not report a cycle`() {
        val graph = mapOf(
            1L to listOf(2L),
            2L to emptyList(),
            3L to listOf(4L),
            4L to emptyList()
        )

        assertFalse(TaskDependencyValidator.hasCycle(graph))
    }
}
