package com.example.projecttracker.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.local.entity.TaskStatus
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskDependencyRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.databinding.FragmentTaskListBinding
import com.example.projecttracker.ui.adapter.TaskListAdapter
import com.example.projecttracker.viewmodel.TaskViewModel
import com.example.projecttracker.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private val projectId: Long
        get() = arguments?.getLong("projectId", -1L) ?: -1L

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

    private val taskListAdapter = TaskListAdapter(
        onEditClick = { task ->
            TaskFormDialogFragment.newInstanceForEdit(projectId, task.id)
                .show(childFragmentManager, TaskFormDialogFragment.TAG)
        },
        onDeleteClick = { task ->
            TaskDeleteConfirmDialogFragment.newInstance(projectId, task.id, task.nama)
                .show(childFragmentManager, TaskDeleteConfirmDialogFragment.TAG)
        }
    )

    private val statusFilterOptions: List<TaskStatus?> =
        listOf(null, TaskStatus.DRAFT, TaskStatus.IN_PROGRESS, TaskStatus.DONE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewTaskList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskListAdapter
        }

        binding.fabAddTask.setOnClickListener {
            TaskFormDialogFragment.newInstanceForAdd(projectId)
                .show(childFragmentManager, TaskFormDialogFragment.TAG)
        }

        setupSearchInput()
        setupStatusFilterDropdown()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.uiState.collect { state ->
                    taskListAdapter.submitList(state.items)
                    updateEmptyState(isEmpty = state.items.isEmpty(), isFilterActive = state.isFilterActive)
                }
            }
        }
    }

    private fun setupSearchInput() {
        binding.editSearchTask.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                taskViewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun setupStatusFilterDropdown() {
        val labels = statusFilterOptions.map { statusFilterLabel(it) }
        binding.editFilterStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.editFilterStatus.setText(labels.first(), false)
        binding.editFilterStatus.setOnItemClickListener { _, _, position, _ ->
            taskViewModel.setStatusFilter(statusFilterOptions[position])
        }
    }

    private fun updateEmptyState(isEmpty: Boolean, isFilterActive: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (!isEmpty) return

        if (isFilterActive) {
            binding.textEmptyStateTitle.text = getString(R.string.task_list_no_results_title)
            binding.textEmptyStateDescription.text = getString(R.string.task_list_no_results_description)
        } else {
            binding.textEmptyStateTitle.text = getString(R.string.task_list_empty_title)
            binding.textEmptyStateDescription.text = getString(R.string.task_list_empty_description)
        }
    }

    private fun statusFilterLabel(status: TaskStatus?): String {
        val resId = when (status) {
            null -> R.string.task_filter_status_all
            TaskStatus.DRAFT -> R.string.task_status_draft
            TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
            TaskStatus.DONE -> R.string.task_status_done
        }
        return getString(resId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewTaskList.adapter = null
        _binding = null
    }
}
