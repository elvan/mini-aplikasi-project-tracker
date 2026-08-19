package com.example.projecttracker.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.viewmodel.ProjectViewModel
import com.example.projecttracker.viewmodel.ProjectViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProjectDeleteConfirmDialogFragment : DialogFragment() {

    private val projectViewModel: ProjectViewModel by viewModels {
        val projectDao = AppDatabase.getInstance(requireContext()).projectDao()
        ProjectViewModelFactory(ProjectRepository(projectDao))
    }

    private val projectId: Long by lazy { requireArguments().getLong(ARG_PROJECT_ID) }
    private val projectName: String by lazy { requireArguments().getString(ARG_PROJECT_NAME).orEmpty() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_project_confirm)
            .setMessage(getString(R.string.message_delete_project_confirm, projectName))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                projectViewModel.deleteProject(projectId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()

    companion object {
        const val TAG = "ProjectDeleteConfirmDialogFragment"
        private const val ARG_PROJECT_ID = "arg_project_id"
        private const val ARG_PROJECT_NAME = "arg_project_name"

        fun newInstance(projectId: Long, projectName: String): ProjectDeleteConfirmDialogFragment =
            ProjectDeleteConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId)
                    putString(ARG_PROJECT_NAME, projectName)
                }
            }
    }
}
