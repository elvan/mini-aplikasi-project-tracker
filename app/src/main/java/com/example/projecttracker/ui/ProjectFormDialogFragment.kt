package com.example.projecttracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.databinding.DialogProjectFormBinding
import com.example.projecttracker.viewmodel.ProjectViewModel
import com.example.projecttracker.viewmodel.ProjectViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ProjectFormDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogProjectFormBinding? = null
    private val binding get() = _binding!!

    private val projectViewModel: ProjectViewModel by viewModels {
        val projectDao = AppDatabase.getInstance(requireContext()).projectDao()
        ProjectViewModelFactory(ProjectRepository(projectDao))
    }

    private val projectId: Long by lazy { arguments?.getLong(ARG_PROJECT_ID, NO_ID) ?: NO_ID }
    private val isEditMode: Boolean get() = projectId != NO_ID

    private var editingProject: Project? = null
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProjectFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textFormTitle.text = getString(
            if (isEditMode) R.string.title_project_form_edit else R.string.title_project_form_add
        )
        binding.layoutReadonlyInfo.visibility = if (isEditMode) View.VISIBLE else View.GONE

        binding.editStartDate.setOnClickListener { showDatePicker(isStart = true) }
        binding.editEndDate.setOnClickListener { showDatePicker(isStart = false) }

        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonSave.setOnClickListener { onSaveClicked() }

        if (isEditMode) {
            loadProject()
        }
    }

    private fun loadProject() {
        viewLifecycleOwner.lifecycleScope.launch {
            val project = projectViewModel.getProjectById(projectId)
            if (project == null) {
                dismiss()
                return@launch
            }
            editingProject = project
            startDate = project.startDate
            endDate = project.endDate

            binding.editProjectName.setText(project.nama)
            binding.editStartDate.setText(project.startDate.format(dateFormatter))
            binding.editEndDate.setText(project.endDate.format(dateFormatter))
            binding.textStatusValue.text = statusLabel(project.status)
            binding.textProgressValue.text =
                getString(R.string.project_progress_format, project.completionProgress)
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val currentSelection = (if (isStart) startDate else endDate)?.toUtcMillis()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) R.string.label_start_date else R.string.label_end_date)
            .setSelection(currentSelection)
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val selectedDate = selectionMillis.toLocalDateUtc()
            if (isStart) {
                startDate = selectedDate
                binding.editStartDate.setText(selectedDate.format(dateFormatter))
                binding.tilStartDate.error = null
            } else {
                endDate = selectedDate
                binding.editEndDate.setText(selectedDate.format(dateFormatter))
                binding.tilEndDate.error = null
            }
        }
        picker.show(childFragmentManager, if (isStart) TAG_START_DATE_PICKER else TAG_END_DATE_PICKER)
    }

    private fun onSaveClicked() {
        val name = binding.editProjectName.text?.toString()?.trim().orEmpty()
        val start = startDate
        val end = endDate

        var hasError = false

        if (name.isEmpty()) {
            binding.tilProjectName.error = getString(R.string.error_project_name_required)
            hasError = true
        } else {
            binding.tilProjectName.error = null
        }

        if (start == null) {
            binding.tilStartDate.error = getString(R.string.error_date_required)
            hasError = true
        } else {
            binding.tilStartDate.error = null
        }

        if (end == null) {
            binding.tilEndDate.error = getString(R.string.error_date_required)
            hasError = true
        } else {
            binding.tilEndDate.error = null
        }

        if (start != null && end != null && end.isBefore(start)) {
            binding.tilEndDate.error = getString(R.string.error_end_date_before_start_date)
            hasError = true
        }

        if (hasError || start == null || end == null) return

        val existing = editingProject
        if (existing != null) {
            projectViewModel.updateProject(existing.copy(nama = name, startDate = start, endDate = end))
        } else {
            projectViewModel.addProject(name, start, end)
        }
        dismiss()
    }

    private fun statusLabel(status: ProjectStatus): String {
        val resId = when (status) {
            ProjectStatus.DRAFT -> R.string.project_status_draft
            ProjectStatus.IN_PROGRESS -> R.string.project_status_in_progress
            ProjectStatus.DONE -> R.string.project_status_done
        }
        return getString(resId)
    }

    private fun LocalDate.toUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun Long.toLocalDateUtc(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ProjectFormDialogFragment"
        private const val ARG_PROJECT_ID = "arg_project_id"
        private const val NO_ID = -1L
        private const val TAG_START_DATE_PICKER = "start_date_picker"
        private const val TAG_END_DATE_PICKER = "end_date_picker"

        fun newInstanceForAdd(): ProjectFormDialogFragment = ProjectFormDialogFragment()

        fun newInstanceForEdit(projectId: Long): ProjectFormDialogFragment =
            ProjectFormDialogFragment().apply {
                arguments = Bundle().apply { putLong(ARG_PROJECT_ID, projectId) }
            }
    }
}
