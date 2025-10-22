package ca.unb.mobiledev.handyhub.home.presentation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.FragmentHomeBinding
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.HomeViewModel
import ca.unb.mobiledev.handyhub.home.presentation.adapter.ServiceAdapter
import ca.unb.mobiledev.handyhub.home.presentation.anim.GreetingAnimator
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var serviceAdapter: ServiceAdapter
    private val viewModel: HomeViewModel by viewModels()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        animateGreeting()
        animateSearchBar()
    }
    
    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdapter { service ->
            // Handle service click
            // TODO: Navigate to service details
        }
        
        binding.recyclerViewServices.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = serviceAdapter
        }
        
        // Observe services from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.services.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Show loading state if needed
                    }
                    is Resource.Success -> {
                        serviceAdapter.submitList(resource.data)
                    }
                    is Resource.Error -> {
                        // Handle error state
                        serviceAdapter.submitList(emptyList())
                    }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonAll.setOnClickListener {
            updateFilterSelection("All")
        }
        
        binding.buttonHome.setOnClickListener {
            updateFilterSelection("Home")
        }
        
        binding.buttonOutdoor.setOnClickListener {
            updateFilterSelection("Outdoor")
        }
    }
    
    private fun updateFilterSelection(selectedFilter: String) {
        val animatorSet = AnimatorSet()
        val animations = mutableListOf<ObjectAnimator>()

        val currentSelected = when (selectedFilter) {
            "All" -> binding.buttonAll
            "Home" -> binding.buttonHome
            "Outdoor" -> binding.buttonOutdoor
            else -> binding.buttonAll
        }

        listOf(binding.buttonAll, binding.buttonHome, binding.buttonOutdoor).forEach { button ->
            if (button != currentSelected) {
                val colorAnimator = ObjectAnimator.ofArgb(
                    button,
                    "textColor",
                    button.currentTextColor,
                    resources.getColor(android.R.color.white, null)
                )
                colorAnimator.duration = 200
                colorAnimator.interpolator = DecelerateInterpolator()
                animations.add(colorAnimator)

                val scaleXAnimator = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 0.95f, 1.0f)
                val scaleYAnimator = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 0.95f, 1.0f)
                scaleXAnimator.duration = 200
                scaleYAnimator.duration = 200
                scaleXAnimator.interpolator = DecelerateInterpolator()
                scaleYAnimator.interpolator = DecelerateInterpolator()
                animations.add(scaleXAnimator)
                animations.add(scaleYAnimator)
            }
        }

        val selectedColorAnimator = ObjectAnimator.ofArgb(
            currentSelected,
            "textColor",
            currentSelected.currentTextColor,
            resources.getColor(android.R.color.black, null)
        )
        selectedColorAnimator.duration = 250
        selectedColorAnimator.interpolator = DecelerateInterpolator()
        animations.add(selectedColorAnimator)

        val selectedScaleXAnimator = ObjectAnimator.ofFloat(currentSelected, "scaleX", 1.0f, 1.05f, 1.0f)
        val selectedScaleYAnimator = ObjectAnimator.ofFloat(currentSelected, "scaleY", 1.0f, 1.05f, 1.0f)
        selectedScaleXAnimator.duration = 250
        selectedScaleYAnimator.duration = 250
        selectedScaleXAnimator.interpolator = DecelerateInterpolator()
        selectedScaleYAnimator.interpolator = DecelerateInterpolator()
        animations.add(selectedScaleXAnimator)
        animations.add(selectedScaleYAnimator)

        animatorSet.playTogether(animations as List<android.animation.Animator>)
        
        binding.buttonAll.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonAll.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonAll.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.buttonHome.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonHome.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonHome.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.buttonOutdoor.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonOutdoor.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonOutdoor.setBackgroundResource(R.drawable.tab_unselected_background)

        when (selectedFilter) {
            "All" -> {
                binding.buttonAll.setTextColor(resources.getColor(android.R.color.black, null))
                binding.buttonAll.typeface = resources.getFont(R.font.inter_18pt_semibold_ttf)
                binding.buttonAll.setBackgroundResource(R.drawable.tab_selected_background)
            }
            "Home" -> {
                binding.buttonHome.setTextColor(resources.getColor(android.R.color.black, null))
                binding.buttonHome.typeface = resources.getFont(R.font.inter_18pt_semibold_ttf)
                binding.buttonHome.setBackgroundResource(R.drawable.tab_selected_background)
            }
            "Outdoor" -> {
                binding.buttonOutdoor.setTextColor(resources.getColor(android.R.color.black, null))
                binding.buttonOutdoor.typeface = resources.getFont(R.font.inter_18pt_semibold_ttf)
                binding.buttonOutdoor.setBackgroundResource(R.drawable.tab_selected_background)
            }
        }
        
        animatorSet.start()
    }

    private fun animateGreeting() {
        GreetingAnimator.animateGreetingSlideIn(binding.greetingContainer)
    }

    private fun animateSearchBar() {
        GreetingAnimator.animateSearchBarExpand(binding.searchBarContainer)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

