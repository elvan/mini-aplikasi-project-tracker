package com.example.projecttracker.ui.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projecttracker.R
import com.example.projecttracker.data.local.entity.Task
import com.example.projecttracker.data.local.entity.TaskStatus
import com.example.projecttracker.databinding.ItemTaskBinding
import com.example.projecttracker.viewmodel.TaskListItem

class TaskListAdapter(
    private val onEditClick: (Task) -> Unit
) : ListAdapter<TaskListItem, TaskListAdapter.TaskViewHolder>(TaskDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick)
    }

    class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaskListItem, onEditClick: (Task) -> Unit) {
            val context = binding.root.context

            binding.textTaskName.text = item.task.nama
            binding.textTaskStatus.text = statusLabel(item.task.status)
            binding.textTaskBobot.text = context.getString(R.string.task_item_bobot_format, item.task.bobot)
            binding.buttonEditTask.setOnClickListener { onEditClick(item.task) }

            binding.cardTask.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = dpToPx(16) + item.level * dpToPx(24)
            }
        }

        private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            binding.root.resources.displayMetrics
        ).toInt()

        private fun statusLabel(status: TaskStatus): String {
            val resId = when (status) {
                TaskStatus.DRAFT -> R.string.task_status_draft
                TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
                TaskStatus.DONE -> R.string.task_status_done
            }
            return binding.root.context.getString(resId)
        }
    }

    private object TaskDiffCallback : DiffUtil.ItemCallback<TaskListItem>() {
        override fun areItemsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean =
            oldItem.task.id == newItem.task.id

        override fun areContentsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean =
            oldItem == newItem
    }
}
