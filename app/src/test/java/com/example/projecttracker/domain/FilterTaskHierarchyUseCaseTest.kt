package com.example.projecttracker.domain

import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTaskHierarchyUseCaseTest {

    // Hierarchy used across tests:
    // 1 Setup API (Draft)
    //   2 Design API schema (In Progress)
    //   3 Implement API (Done)
    //     4 Write API tests (In Progress)
    // 5 Setup UI (Done)
    //   6 Build UI components (Draft)
    private val tasks = listOf(
        Task(id = 1, nama = "Setup API", status = TaskStatus.DRAFT, projectId = 1, bobot = 1),
        Task(id = 2, nama = "Design API schema", status = TaskStatus.IN_PROGRESS, projectId = 1, bobot = 1, parentTaskId = 1),
        Task(id = 3, nama = "Implement API", status = TaskStatus.DONE, projectId = 1, bobot = 1, parentTaskId = 1),
        Task(id = 4, nama = "Write API tests", status = TaskStatus.IN_PROGRESS, projectId = 1, bobot = 1, parentTaskId = 3),
        Task(id = 5, nama = "Setup UI", status = TaskStatus.DONE, projectId = 1, bobot = 1),
        Task(id = 6, nama = "Build UI components", status = TaskStatus.DRAFT, projectId = 1, bobot = 1, parentTaskId = 5)
    )

    @Test
    fun `no filter and no query returns every task`() {
        val result = FilterTaskHierarchyUseCase.filter(tasks, statusFilter = null, query = "")

        assertEquals(tasks.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    @Test
    fun `status filter alone keeps matching tasks plus their ancestor chain`() {
        val result = FilterTaskHierarchyUseCase.filter(tasks, statusFilter = TaskStatus.IN_PROGRESS, query = "")

        // 2 and 4 match directly; 1 and 3 are pulled in as ancestors of 4 / 2. 5, 6 are excluded.
        assertEquals(setOf(1L, 2L, 3L, 4L), result.map { it.id }.toSet())
    }

    @Test
    fun `name search alone keeps matching tasks plus their ancestor chain`() {
        val result = FilterTaskHierarchyUseCase.filter(tasks, statusFilter = null, query = "API")

        assertEquals(setOf(1L, 2L, 3L, 4L), result.map { it.id }.toSet())
    }

    @Test
    fun `name search is case insensitive and trims whitespace`() {
        val result = FilterTaskHierarchyUseCase.filter(tasks, statusFilter = null, query = "  ui  ")

        assertEquals(setOf(5L, 6L), result.map { it.id }.toSet())
    }

    @Test
    fun `combined status filter and search require both to match`() {
        val result = FilterTaskHierarchyUseCase.filter(
            tasks,
            statusFilter = TaskStatus.IN_PROGRESS,
            query = "API"
        )

        // Only 2 and 4 are In Progress AND contain "API"; 1 and 3 are kept as ancestors.
        assertEquals(setOf(1L, 2L, 3L, 4L), result.map { it.id }.toSet())
    }

    @Test
    fun `combined filter with no matches returns an empty result`() {
        val result = FilterTaskHierarchyUseCase.filter(
            tasks,
            statusFilter = TaskStatus.DONE,
            query = "UI components"
        )

        assertEquals(emptySet<Long>(), result.map { it.id }.toSet())
    }

    @Test
    fun `subtask match without parent match still surfaces the parent`() {
        // Task 4 ("Write API tests") is In Progress; its parent (3, Done) and grandparent
        // (1, Draft) don't match the filter themselves but must still be present.
        val result = FilterTaskHierarchyUseCase.filter(tasks, statusFilter = TaskStatus.IN_PROGRESS, query = "tests")

        assertEquals(setOf(1L, 3L, 4L), result.map { it.id }.toSet())
    }

    @Test
    fun `deep grandchild match pulls in the full ancestor chain`() {
        val deepTasks = tasks + Task(
            id = 7,
            nama = "Write edge case tests",
            status = TaskStatus.DRAFT,
            projectId = 1,
            bobot = 1,
            parentTaskId = 4
        )

        val result = FilterTaskHierarchyUseCase.filter(deepTasks, statusFilter = null, query = "edge case")

        assertEquals(setOf(1L, 3L, 4L, 7L), result.map { it.id }.toSet())
    }
}
