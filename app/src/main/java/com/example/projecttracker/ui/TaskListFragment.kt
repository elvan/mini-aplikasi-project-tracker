package com.example.projecttracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecttracker.data.local.AppDatabase
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
        val taskDao = AppDatabase.getInstance(requireContext()).taskDao()
        TaskViewModelFactory(TaskRepository(taskDao), projectId)
    }

    private val taskListAdapter = TaskListAdapter()

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.tasks.collect { items ->
                    taskListAdapter.submitList(items)
                    binding.layoutEmptyState.visibility = if (items.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewTaskList.adapter = null
        _binding = null
    }
}
