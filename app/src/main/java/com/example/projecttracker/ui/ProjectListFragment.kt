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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecttracker.R
import com.example.projecttracker.data.local.AppDatabase
import com.example.projecttracker.data.repository.ProjectDependencyRepository
import com.example.projecttracker.data.repository.ProjectRepository
import com.example.projecttracker.data.repository.TaskRepository
import com.example.projecttracker.databinding.FragmentProjectListBinding
import com.example.projecttracker.ui.adapter.ProjectListAdapter
import com.example.projecttracker.viewmodel.ProjectViewModel
import com.example.projecttracker.viewmodel.ProjectViewModelFactory
import kotlinx.coroutines.launch

class ProjectListFragment : Fragment() {

    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!

    private val projectViewModel: ProjectViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ProjectViewModelFactory(
            ProjectRepository(db.projectDao()),
            TaskRepository(db.taskDao()),
            ProjectDependencyRepository(db.projectDependencyDao())
        )
    }

    private val projectListAdapter = ProjectListAdapter(
        onItemClick = { project ->
            val args = Bundle().apply { putLong("projectId", project.id) }
            findNavController().navigate(R.id.action_projectListFragment_to_taskListFragment, args)
        },
        onEditClick = { project ->
            ProjectFormDialogFragment.newInstanceForEdit(project.id)
                .show(childFragmentManager, ProjectFormDialogFragment.TAG)
        },
        onDeleteClick = { project ->
            ProjectDeleteConfirmDialogFragment.newInstance(project.id, project.nama)
                .show(childFragmentManager, ProjectDeleteConfirmDialogFragment.TAG)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewProjectList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projectListAdapter
        }

        binding.fabAddProject.setOnClickListener {
            ProjectFormDialogFragment.newInstanceForAdd()
                .show(childFragmentManager, ProjectFormDialogFragment.TAG)
        }

        binding.swipeRefreshProjectList.setOnRefreshListener {
            projectViewModel.refreshProjects()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    projectViewModel.projects.collect { projects ->
                        projectListAdapter.submitList(projects)
                        binding.layoutEmptyState.visibility = if (projects.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                launch {
                    projectViewModel.isDeleting.collect { isDeleting ->
                        binding.layoutDeleteLoading.visibility = if (isDeleting) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                launch {
                    projectViewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefreshProjectList.isRefreshing = isRefreshing
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewProjectList.adapter = null
        _binding = null
    }
}
