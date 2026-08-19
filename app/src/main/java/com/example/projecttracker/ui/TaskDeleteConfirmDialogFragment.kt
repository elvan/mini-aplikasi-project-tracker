package com.example.projecttracker.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskDependencyRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.viewmodel.TaskViewModel
import com.example.projecttracker.viewmodel.TaskViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TaskDeleteConfirmDialogFragment : DialogFragment() {

    private val projectId: Long by lazy { requireArguments().getLong(ARG_PROJECT_ID) }
    private val taskId: Long by lazy { requireArguments().getLong(ARG_TASK_ID) }
    private val taskName: String by lazy { requireArguments().getString(ARG_TASK_NAME).orEmpty() }

    private val taskViewModel: TaskViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        TaskViewModelFactory(
            TaskRepository(db.taskDao()),
            TaskDependencyRepository(db.taskDependencyDao()),
            ProjectRepository(db.projectDao()),
            ProjectDependencyRepository(db.projectDependencyDao()),
            projectId
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_task_confirm)
            .setMessage(getString(R.string.message_delete_task_confirm, taskName))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                taskViewModel.deleteTask(taskId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()

    companion object {
        const val TAG = "TaskDeleteConfirmDialogFragment"
        private const val ARG_PROJECT_ID = "arg_project_id"
        private const val ARG_TASK_ID = "arg_task_id"
        private const val ARG_TASK_NAME = "arg_task_name"

        fun newInstance(projectId: Long, taskId: Long, taskName: String): TaskDeleteConfirmDialogFragment =
            TaskDeleteConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId)
                    putLong(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_NAME, taskName)
                }
            }
    }
}
