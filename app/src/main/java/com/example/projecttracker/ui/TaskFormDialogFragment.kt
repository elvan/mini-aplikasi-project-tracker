package com.example.projecttracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskDependencyRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.databinding.DialogTaskFormBinding
import com.example.projecttracker.viewmodel.TaskSaveError
import com.example.projecttracker.viewmodel.TaskSaveResult
import com.example.projecttracker.viewmodel.TaskViewModel
import com.example.projecttracker.viewmodel.TaskViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TaskFormDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogTaskFormBinding? = null
    private val binding get() = _binding!!

    private val projectId: Long by lazy { requireArguments().getLong(ARG_PROJECT_ID) }
    private val taskId: Long? by lazy {
        val id = arguments?.getLong(ARG_TASK_ID, NO_ID) ?: NO_ID
        if (id == NO_ID) null else id
    }
    private val isEditMode: Boolean get() = taskId != null

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

    private val statusOptions = listOf(TaskStatus.DRAFT, TaskStatus.IN_PROGRESS, TaskStatus.DONE)
    private var selectedStatus: TaskStatus = TaskStatus.DRAFT

    private var parentOptions: List<Task> = emptyList()
    private var selectedParentTaskId: Long? = null

    private var dependencyOptions: List<Task> = emptyList()
    private var selectedDependencyIds: MutableSet<Long> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTaskFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textFormTitle.text = getString(
            if (isEditMode) R.string.title_task_form_edit else R.string.title_task_form_add
        )

        setupStatusDropdown()
        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonSave.setOnClickListener { onSaveClicked() }
        binding.editDependency.setOnClickListener { showDependencyPicker() }

        loadFormData()
    }

    private fun setupStatusDropdown() {
        val labels = statusOptions.map { statusLabel(it) }
        binding.editStatus.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        binding.editStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statusOptions[position]
        }
    }

    private fun loadFormData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allTasks = taskViewModel.getAllTasksInProjectOnce()
            val existing = taskId?.let { taskViewModel.getTaskById(it) }
            if (isEditMode && existing == null) {
                dismiss()
                return@launch
            }

            val excludedFromParent = if (existing != null) {
                descendantIds(existing.id, allTasks) + existing.id
            } else {
                emptySet()
            }
            parentOptions = allTasks.filter { it.id !in excludedFromParent }
            dependencyOptions = allTasks.filter { it.id != existing?.id }

            setupParentDropdown()

            selectedStatus = existing?.status ?: TaskStatus.DRAFT
            binding.editStatus.setText(statusLabel(selectedStatus), false)

            selectedParentTaskId = existing?.parentTaskId
            binding.editParentTask.setText(
                parentOptions.firstOrNull { it.id == selectedParentTaskId }?.nama
                    ?: getString(R.string.option_parent_none),
                false
            )

            if (existing != null) {
                binding.editTaskName.setText(existing.nama)
                binding.editBobot.setText(existing.bobot.toString())
                selectedDependencyIds = taskViewModel.getDependencyIdsOf(existing.id).toMutableSet()
            }
            updateDependencySummary()
        }
    }

    private fun setupParentDropdown() {
        val labels = listOf(getString(R.string.option_parent_none)) + parentOptions.map { it.nama }
        binding.editParentTask.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        binding.editParentTask.setOnItemClickListener { _, _, position, _ ->
            selectedParentTaskId = if (position == 0) null else parentOptions[position - 1].id
        }
    }

    private fun showDependencyPicker() {
        if (dependencyOptions.isEmpty()) return
        val labels = dependencyOptions.map { it.nama }.toTypedArray()
        val checked = dependencyOptions.map { it.id in selectedDependencyIds }.toBooleanArray()
        val tempSelected = selectedDependencyIds.toMutableSet()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_task_dependency)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val id = dependencyOptions[which].id
                if (isChecked) tempSelected += id else tempSelected -= id
            }
            .setPositiveButton(R.string.action_save) { _, _ ->
                selectedDependencyIds = tempSelected
                updateDependencySummary()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun updateDependencySummary() {
        binding.editDependency.setText(
            if (selectedDependencyIds.isEmpty()) {
                getString(R.string.dependency_none_selected)
            } else {
                dependencyOptions.filter { it.id in selectedDependencyIds }.joinToString(", ") { it.nama }
            }
        )
    }

    private fun onSaveClicked() {
        val name = binding.editTaskName.text?.toString()?.trim().orEmpty()
        val bobotText = binding.editBobot.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (name.isEmpty()) {
            binding.tilTaskName.error = getString(R.string.error_task_name_required)
            hasError = true
        } else {
            binding.tilTaskName.error = null
        }

        val bobot = bobotText.toIntOrNull()
        if (bobot == null || bobot <= 0) {
            binding.tilBobot.error = getString(R.string.error_task_bobot_invalid)
            hasError = true
        } else {
            binding.tilBobot.error = null
        }

        if (hasError || bobot == null) return

        binding.buttonSave.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = taskViewModel.saveTask(
                taskId = taskId,
                nama = name,
                status = selectedStatus,
                bobot = bobot,
                parentTaskId = selectedParentTaskId,
                dependencyIds = selectedDependencyIds
            )
            when (result) {
                is TaskSaveResult.Success -> {
                    if (result.invalidatedDependentNames.isNotEmpty()) {
                        showInvalidatedDependentsWarning(result.invalidatedDependentNames)
                    }
                    dismiss()
                }
                is TaskSaveResult.Error -> {
                    binding.buttonSave.isEnabled = true
                    showSaveError(result.error)
                }
            }
        }
    }

    private fun showSaveError(error: TaskSaveError) {
        val message = when (error) {
            is TaskSaveError.DependencyNotDone ->
                getString(R.string.error_task_dependency_not_done, error.dependencyName)
            TaskSaveError.CircularDependency ->
                getString(R.string.error_task_circular_dependency)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showInvalidatedDependentsWarning(dependentNames: List<String>) {
        Toast.makeText(
            requireContext(),
            getString(R.string.warning_task_dependents_invalidated, dependentNames.joinToString(", ")),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun statusLabel(status: TaskStatus): String {
        val resId = when (status) {
            TaskStatus.DRAFT -> R.string.task_status_draft
            TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
            TaskStatus.DONE -> R.string.task_status_done
        }
        return getString(resId)
    }

    private fun descendantIds(rootTaskId: Long, tasks: List<Task>): Set<Long> {
        val childrenByParent = tasks.groupBy { it.parentTaskId }
        val result = mutableSetOf<Long>()

        fun collect(id: Long) {
            for (child in childrenByParent[id].orEmpty()) {
                if (result.add(child.id)) collect(child.id)
            }
        }

        collect(rootTaskId)
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskFormDialogFragment"
        private const val ARG_PROJECT_ID = "arg_project_id"
        private const val ARG_TASK_ID = "arg_task_id"
        private const val NO_ID = -1L

        fun newInstanceForAdd(projectId: Long): TaskFormDialogFragment =
            TaskFormDialogFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PROJECT_ID, projectId) }
            }

        fun newInstanceForEdit(projectId: Long, taskId: Long): TaskFormDialogFragment =
            TaskFormDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PROJECT_ID, projectId)
                    putLong(ARG_TASK_ID, taskId)
                }
            }
    }
}
