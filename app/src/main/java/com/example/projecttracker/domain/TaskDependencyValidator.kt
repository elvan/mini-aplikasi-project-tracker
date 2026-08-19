package com.example.projecttracker.domain

object TaskDependencyValidator {

    // DFS with a recursion-stack marker so indirect cycles (A->B->C->A) are caught, not just direct ones.
    fun hasCycle(graph: Map<Long, List<Long>>): Boolean {
        val visited = mutableSetOf<Long>()
        val inStack = mutableSetOf<Long>()

        fun dfs(node: Long): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited += node
            inStack += node
            for (neighbor in graph[node].orEmpty()) {
                if (dfs(neighbor)) return true
            }
            inStack -= node
            return false
        }

        return graph.keys.any { dfs(it) }
    }
}
