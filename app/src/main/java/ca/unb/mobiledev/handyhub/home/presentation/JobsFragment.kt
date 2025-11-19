package ca.unb.mobiledev.handyhub.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.databinding.FragmentJobsBinding
import ca.unb.mobiledev.handyhub.home.domain.model.JobMilestone
import ca.unb.mobiledev.handyhub.home.presentation.adapter.TimelineAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JobsFragment : Fragment() {
    
    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var timelineAdapter: TimelineAdapter
    private var currentTestState = 0
    
    private val testStates = listOf(
        listOf(
            JobMilestone("Service Order Accepted", "2:30 PM", true, false),
            JobMilestone("Service Provider is on the Way", "2:45 PM", false, true),
            JobMilestone("Reached Service Destination", "", false, false),
            JobMilestone("Service in Progress", "", false, false),
            JobMilestone("Service Completed", "", false, false)
        ),
        listOf(
            JobMilestone("Service Order Accepted", "2:30 PM", true, false),
            JobMilestone("Service Provider is on the Way", "2:45 PM", true, false),
            JobMilestone("Reached Service Destination", "3:00 PM", false, true),
            JobMilestone("Service in Progress", "", false, false),
            JobMilestone("Service Completed", "", false, false)
        ),
        listOf(
            JobMilestone("Service Order Accepted", "2:30 PM", true, false),
            JobMilestone("Service Provider is on the Way", "2:45 PM", true, false),
            JobMilestone("Reached Service Destination", "3:00 PM", true, false),
            JobMilestone("Service in Progress", "3:15 PM", false, true),
            JobMilestone("Service Completed", "", false, false)
        ),
        listOf(
            JobMilestone("Service Order Accepted", "2:30 PM", true, false),
            JobMilestone("Service Provider is on the Way", "2:45 PM", true, false),
            JobMilestone("Reached Service Destination", "3:00 PM", true, false),
            JobMilestone("Service in Progress", "3:15 PM", true, false),
            JobMilestone("Service Completed", "4:00 PM", true, false)
        )
    )
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTimelineRecyclerView()
        setupClickListeners()
        showEmptyState()
    }
    
    private fun setupTimelineRecyclerView() {
        timelineAdapter = TimelineAdapter()
        binding.recyclerViewTimeline.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonToggleView.setOnClickListener {
            if (binding.cardViewTimeline.visibility == View.VISIBLE) {
                currentTestState = (currentTestState + 1) % testStates.size
                showTimeline(testStates[currentTestState])
            } else {
                showTimeline(testStates[currentTestState])
            }
        }
        
        binding.includeEmptyState.buttonFindServices.setOnClickListener {
            // TODO: Navigate to services screen
        }
    }
    
    private fun showEmptyState() {
        binding.cardViewTimeline.visibility = View.GONE
        binding.includeEmptyState.root.visibility = View.VISIBLE
    }
    
    private fun showTimeline(milestones: List<JobMilestone>) {
        binding.cardViewTimeline.visibility = View.VISIBLE
        binding.includeEmptyState.root.visibility = View.GONE
        timelineAdapter.submitList(milestones)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

