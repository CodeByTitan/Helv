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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.FragmentHomeBinding
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.HomeViewModel
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.SearchViewModel
import ca.unb.mobiledev.handyhub.home.presentation.adapter.SearchResultAdapter
import ca.unb.mobiledev.handyhub.home.presentation.adapter.ServiceAdapter
import ca.unb.mobiledev.handyhub.home.presentation.anim.GreetingAnimator
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    
    companion object {
        private var animationsShown = false
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchRecyclerView()
        setupClickListeners()
        setupSearchListener()
        observeUserName()
        observeSearchResults()
        
        // Only run animations on first launch (after login/signup)
        if (!animationsShown) {
            animateGreeting()
            animateSearchBar()
            animationsShown = true
        }
    }
    
    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdapter { service ->
            val bundle = Bundle().apply {
                putString("serviceId", service.id)
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_servicePickerFragment,
                bundle
            )
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
    
    private fun setupSearchRecyclerView() {
        searchResultAdapter = SearchResultAdapter { result ->
            val bundle = Bundle().apply {
                putString("serviceId", result.serviceId)
            }
            findNavController().navigate(
                R.id.action_homeFragment_to_servicePickerFragment,
                bundle
            )
            binding.searchEditText.text?.clear()
            binding.recyclerViewSearchResults.visibility = View.GONE
        }
        
        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultAdapter
        }
    }
    
    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) {
                    searchViewModel.search(query)
                } else {
                    searchViewModel.clearSearch()
                    binding.recyclerViewSearchResults.visibility = View.GONE
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            searchViewModel.searchResults.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.recyclerViewSearchResults.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        val results = resource.data ?: emptyList()
                        if (results.isNotEmpty()) {
                            binding.recyclerViewSearchResults.visibility = View.VISIBLE
                            searchResultAdapter.submitList(results)
                        } else {
                            binding.recyclerViewSearchResults.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        binding.recyclerViewSearchResults.visibility = View.GONE
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
    
    private fun observeUserName() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userName.collect { name ->
                name?.let {
                    binding.textGoodMorning.text = "Welcome, $it"
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

