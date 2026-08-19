package com.example.projecttracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectCircularDependencyUseCaseTest {

    @Test
    fun `adding a direct circular dependency is rejected - A depends on B, B depends on A`() {
        // A (id 1) already depends on nothing; B (id 2) already depends on A.
        val existingEdges = mapOf(2L to listOf(1L))

        val wouldCycle = DetectCircularDependencyUseCase.wouldCreateCycle(
            allTaskIds = listOf(1L, 2L),
            existingDependencyEdges = existingEdges,
            editedTaskId = 1L,
            newDependencyIds = setOf(2L)
        )

        assertTrue(wouldCycle)
    }

    @Test
    fun `adding an indirect circular dependency across 3 nodes is rejected`() {
        // B depends on C, C depends on A. Making A depend on B closes the cycle A->B->C->A.
        val existingEdges = mapOf(
            2L to listOf(3L),
            3L to listOf(1L)
        )

        val wouldCycle = DetectCircularDependencyUseCase.wouldCreateCycle(
            allTaskIds = listOf(1L, 2L, 3L),
            existingDependencyEdges = existingEdges,
            editedTaskId = 1L,
            newDependencyIds = setOf(2L)
        )

        assertTrue(wouldCycle)
    }

    @Test
    fun `non-circular dependency chain is accepted`() {
        val existingEdges = mapOf(2L to listOf(3L))

        val wouldCycle = DetectCircularDependencyUseCase.wouldCreateCycle(
            allTaskIds = listOf(1L, 2L, 3L),
            existingDependencyEdges = existingEdges,
            editedTaskId = 1L,
            newDependencyIds = setOf(2L)
        )

        assertFalse(wouldCycle)
    }

    @Test
    fun `unrelated existing edges do not cause a false positive`() {
        val existingEdges = mapOf(3L to listOf(4L))

        val wouldCycle = DetectCircularDependencyUseCase.wouldCreateCycle(
            allTaskIds = listOf(1L, 2L, 3L, 4L),
            existingDependencyEdges = existingEdges,
            editedTaskId = 1L,
            newDependencyIds = setOf(2L)
        )

        assertFalse(wouldCycle)
    }
}
