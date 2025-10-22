package ca.unb.mobiledev.handyhub

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import ca.unb.mobiledev.handyhub.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupNavigation()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        setupNavigationItem(binding.navHome, R.drawable.home, "Services", true)
        setupNavigationItem(binding.navServices, R.drawable.worker, "Jobs", false)
        setupNavigationItem(binding.navMessages, R.drawable.message, "Messages", false)
        setupNavigationItem(binding.navProfile, R.drawable.user, "Profile", false)
        
        binding.navHome.root.setOnClickListener {
            navController.navigate(R.id.homeFragment)
            updateNavigationSelection(0)
        }
        
        binding.navServices.root.setOnClickListener {
            navController.navigate(R.id.jobsFragment)
            updateNavigationSelection(1)
        }
        
        binding.navMessages.root.setOnClickListener {
            navController.navigate(R.id.messagesFragment)
            updateNavigationSelection(2)
        }
        
        binding.navProfile.root.setOnClickListener {
            navController.navigate(R.id.profileFragment)
            updateNavigationSelection(3)
        }
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
    
    private fun updateNavigationSelection(selectedIndex: Int) {
        val navItems = listOf(binding.navHome, binding.navServices, binding.navMessages, binding.navProfile)
        
        navItems.forEachIndexed { index, navItemBinding ->
            if (index == selectedIndex) {
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
}
