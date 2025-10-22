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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var hasCheckedInitialStatus = false
    private var authStateJob: Job? = null
    private var updateDetailsJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkInitialOnboardingStatus()
        observeAuthenticationChanges()
        observeDetailsUpdate()
    }
    
    private fun checkInitialOnboardingStatus() {
        if (hasCheckedInitialStatus) return
        
        hasCheckedInitialStatus = true
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
                    }
                    is Resource.Error -> {
                        // User not logged in - stay on SignUpFragment (do nothing)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun observeAuthenticationChanges() {
        authStateJob = lifecycleScope.launch {
            var isFirstEmission = true
            authViewModel.authState.collect { resource ->
                // Skip the first emission (initial Loading state)
                if (isFirstEmission) {
                    isFirstEmission = false
                    if (resource is Resource.Loading) {
                        return@collect
                    }
                }
                
                when (resource) {
                    is Resource.Success -> {
                        val user = resource.data
                        if (user != null) {
                            if (user.onboardingCompleted) {
                                navigateToMainActivity()
                            } else {
                                navigateToDetailsFragment()
                            }
                        }
                    }
                    is Resource.Error -> {}
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun observeDetailsUpdate() {
        updateDetailsJob = lifecycleScope.launch {
            var isFirstEmission = true
            authViewModel.updateDetailsState.collect { resource ->
                // Skip the first emission (initial Success(Unit) state)
                if (isFirstEmission) {
                    isFirstEmission = false
                    if (resource is Resource.Success) {
                        return@collect
                    }
                }
                
                when (resource) {
                    is Resource.Success -> {
                        navigateToMainActivity()
                    }
                    is Resource.Error -> {}
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun navigateToMainActivity() {
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
    
    override fun onDestroy() {
        super.onDestroy()
        authStateJob?.cancel()
        updateDetailsJob?.cancel()
    }
}
