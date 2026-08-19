package com.example.projecttracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projecttracker.R
import com.example.projecttracker.data.local.entity.Project
import com.example.projecttracker.data.local.entity.ProjectStatus
import com.example.projecttracker.databinding.ItemProjectBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProjectListAdapter(
    private val onItemClick: (Project) -> Unit,
    private val onEditClick: (Project) -> Unit
) : ListAdapter<Project, ProjectListAdapter.ProjectViewHolder>(ProjectDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

        fun bind(project: Project) {
            val context = binding.root.context

            binding.textProjectName.text = project.nama
            binding.textProjectStatus.text = statusLabel(project.status)

            val progress = project.completionProgress.coerceIn(0.0, 100.0)
            binding.progressCompletion.progress = progress.toInt()
            binding.textProjectProgress.text =
                context.getString(R.string.project_progress_format, progress)

            binding.textProjectDates.text = context.getString(
                R.string.project_date_range_format,
                formatDate(project.startDate),
                formatDate(project.endDate)
            )

            binding.root.setOnClickListener { onItemClick(project) }
            binding.buttonEditProject.setOnClickListener { onEditClick(project) }
        }

        private fun formatDate(date: LocalDate): String = date.format(dateFormatter)

        private fun statusLabel(status: ProjectStatus): String {
            val resId = when (status) {
                ProjectStatus.DRAFT -> R.string.project_status_draft
                ProjectStatus.IN_PROGRESS -> R.string.project_status_in_progress
                ProjectStatus.DONE -> R.string.project_status_done
            }
            return binding.root.context.getString(resId)
        }
    }

    private object ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean =
            oldItem == newItem
    }
}
