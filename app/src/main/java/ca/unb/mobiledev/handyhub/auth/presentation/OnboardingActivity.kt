package ca.unb.mobiledev.handyhub.auth.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import ca.unb.mobiledev.handyhub.MainActivity
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel
import ca.unb.mobiledev.handyhub.databinding.ActivityOnboardingBinding
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var hasCheckedStatus = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkOnboardingStatusOnce()
    }
    
    private fun checkOnboardingStatusOnce() {
        if (hasCheckedStatus) return
        
        hasCheckedStatus = true
        authViewModel.checkOnboardingStatus()
        
        lifecycleScope.launch {
            authViewModel.onboardingStatus.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val isComplete = resource.data ?: false
                        if (isComplete) {
                            navigateToMainActivity()
                        } else {
                            navigateToDetailsFragment()
                        }
                        return@collect
                    }
                    is Resource.Error -> {
                        return@collect
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    private fun navigateToDetailsFragment() {
        binding.root.post {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.onboarding_nav_host) as? NavHostFragment
            val navController = navHostFragment?.navController
            
            if (navController?.currentDestination?.id == R.id.signUpFragment) {
                try {
                    navController.navigate(R.id.action_signUp_to_details)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
