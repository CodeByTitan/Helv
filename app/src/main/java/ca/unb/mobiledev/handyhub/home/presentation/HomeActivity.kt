package ca.unb.mobiledev.handyhub.home.presentation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ActivityHomeBinding
import ca.unb.mobiledev.handyhub.home.presentation.adapter.ServiceAdapter
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.HomeViewModel
import ca.unb.mobiledev.handyhub.home.presentation.anim.GreetingAnimator
import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.util.Resource
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHomeBinding
    private lateinit var serviceAdapter: ServiceAdapter
    private val viewModel: HomeViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
               setupRecyclerView()
               setupClickListeners()
               setupNavigation()
               observeUserName()
               animateGreeting()
               animateSearchBar()
               animateBottomNavigation()
    }
    
    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdapter { service ->
            // Handle service click
            // TODO: Navigate to service details
        }
        
        binding.recyclerViewServices.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 1)
            adapter = serviceAdapter
        }
        
        // Observe services from ViewModel
        lifecycleScope.launch {
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
                        // For now, show empty list
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
        
        // Bottom navigation
        binding.navHome.root.setOnClickListener {
            // Already on home screen
        }
        
        binding.navServices.root.setOnClickListener {
            // Already on services screen
        }
        
        binding.navMessages.root.setOnClickListener {
            // TODO: Navigate to messages
        }
        
        binding.navProfile.root.setOnClickListener {
            // TODO: Navigate to profile
        }

               // Search functionality - now permanently visible
               
               // Location icon as back button
               binding.locationIcon.setOnClickListener {
                   finish() // Go back to previous activity (MainActivity)
               }
    }
    
    private fun updateFilterSelection(selectedFilter: String) {
        // Create animation set for smooth transitions
        val animatorSet = AnimatorSet()
        val animations = mutableListOf<ObjectAnimator>()

        // Get current selected button
        val currentSelected = when (selectedFilter) {
            "All" -> binding.buttonAll
            "Home" -> binding.buttonHome
            "Outdoor" -> binding.buttonOutdoor
            else -> binding.buttonAll
        }

        // Animate all buttons to unselected state
        listOf(binding.buttonAll, binding.buttonHome, binding.buttonOutdoor).forEach { button ->
            if (button != currentSelected) {
                // Animate text color to white
                val colorAnimator = ObjectAnimator.ofArgb(
                    button,
                    "textColor",
                    button.currentTextColor,
                    resources.getColor(android.R.color.white, null)
                )
                colorAnimator.duration = 200
                colorAnimator.interpolator = DecelerateInterpolator()
                animations.add(colorAnimator)

                // Animate scale down slightly
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

        // Animate selected button
        val selectedColorAnimator = ObjectAnimator.ofArgb(
            currentSelected,
            "textColor",
            currentSelected.currentTextColor,
            resources.getColor(android.R.color.black, null)
        )
        selectedColorAnimator.duration = 250
        selectedColorAnimator.interpolator = DecelerateInterpolator()
        animations.add(selectedColorAnimator)

        // Animate scale up for selected button
        val selectedScaleXAnimator = ObjectAnimator.ofFloat(currentSelected, "scaleX", 1.0f, 1.05f, 1.0f)
        val selectedScaleYAnimator = ObjectAnimator.ofFloat(currentSelected, "scaleY", 1.0f, 1.05f, 1.0f)
        selectedScaleXAnimator.duration = 250
        selectedScaleYAnimator.duration = 250
        selectedScaleXAnimator.interpolator = DecelerateInterpolator()
        selectedScaleYAnimator.interpolator = DecelerateInterpolator()
        animations.add(selectedScaleXAnimator)
        animations.add(selectedScaleYAnimator)

        // Start animations
        animatorSet.playTogether(animations as List<android.animation.Animator>)
        
        // Update states immediately for smooth background transition
        // Reset all buttons to unselected state
        binding.buttonAll.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonAll.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonAll.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.buttonHome.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonHome.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonHome.setBackgroundResource(R.drawable.tab_unselected_background)

        binding.buttonOutdoor.setTextColor(resources.getColor(android.R.color.white, null))
        binding.buttonOutdoor.typeface = resources.getFont(R.font.inter_18pt_regular_ttf)
        binding.buttonOutdoor.setBackgroundResource(R.drawable.tab_unselected_background)

        // Set selected button state
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

        // TODO: Filter services based on selection
    }

    private fun animateGreeting() {
        GreetingAnimator.animateGreetingSlideIn(binding.greetingContainer)
    }

    private fun animateSearchBar() {
        GreetingAnimator.animateSearchBarExpand(binding.searchBarContainer)
    }

    private fun animateBottomNavigation() {
        GreetingAnimator.animateBottomNavigationSlideUp(binding.bottomNavigationContainer)
    }
    
    private fun observeUserName() {
        lifecycleScope.launch {
            viewModel.userName.collect { name ->
                name?.let {
                    binding.textGoodMorning.text = "Welcome, $it"
                }
            }
        }
    }
    
    private fun setupNavigation() {
        // Set up navigation items with proper icons and labels
        setupNavigationItem(binding.navHome, R.drawable.home, "Services", true)
        setupNavigationItem(binding.navServices, R.drawable.worker, "Jobs", false)
        setupNavigationItem(binding.navMessages, R.drawable.message, "Messages", false)
        setupNavigationItem(binding.navProfile, R.drawable.user, "Profile", false)
    }
    
    @SuppressLint("SuspiciousIndentation")
    private fun setupNavigationItem(
        navItemBinding: ca.unb.mobiledev.handyhub.databinding.NavItemBinding,
        iconRes: Int,
        label: String,
        isSelected: Boolean
    ) {
        navItemBinding.navIcon.setImageResource(iconRes)
        navItemBinding.navLabel.text = label
        
               if (isSelected) {
                   navItemBinding.navIcon.setColorFilter(resources.getColor(R.color.orange_500, null))
                   navItemBinding.navLabel.setTextColor(resources.getColor(android.R.color.white, null))
                   navItemBinding.indicatorDot.visibility = View.VISIBLE
               } else {
                   navItemBinding.navIcon.setColorFilter(resources.getColor(android.R.color.white, null))
                   navItemBinding.navLabel.setTextColor(resources.getColor(android.R.color.white, null))
                   navItemBinding.indicatorDot.visibility = View.GONE
               }
    }
}
