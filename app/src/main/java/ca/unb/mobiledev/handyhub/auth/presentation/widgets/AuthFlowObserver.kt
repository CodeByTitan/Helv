package ca.unb.mobiledev.handyhub.auth.presentation.widgets

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel
import ca.unb.mobiledev.handyhub.util.Resource
import kotlinx.coroutines.launch

/**
 * Handles all Flow observation and resource state changes
 */
class AuthFlowObserver(
    private val fragment: Fragment,
    private val authViewModel: AuthViewModel
) {
    
    fun observeFlows(
        getStartedManager: AuthCardManager,
        joinUsManager: AuthCardManager,
        getStartedCard: AuthCardView,
        joinUsCard: AuthCardView
    ) {
        observeCardState(getStartedManager, getStartedCard, true)
        observeCardState(joinUsManager, joinUsCard, false)
        observeOtpState()
        observeAuthState()
    }
    
    private fun observeCardState(manager: AuthCardManager, card: AuthCardView, isGetStarted: Boolean) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val flow = if (isGetStarted) authViewModel.getStartedState else authViewModel.joinUsState
            val defaultTitle = if (isGetStarted) "Start getting help" else "Join the Community of Helpers"
            
            flow.collect { state ->
                manager.updateUI(state, card, defaultTitle, "Connect with local service providers")
            }
        }
    }
    
    private fun observeOtpState() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.otpState.collect { resource ->
                when (resource) {
                    is Resource.Success -> showToast("OTP sent")
                    is Resource.Error -> showToast("Failed to send OTP: ${resource.message}")
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun observeAuthState() {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        showToast("Authentication successful")
                    }
                    is Resource.Error -> showToast(resource.message ?: "Authentication failed")
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    fun handleEmailCheckResult(
        getStartedContent: ca.unb.mobiledev.handyhub.databinding.ContentAuthCardBinding,
        joinUsContent: ca.unb.mobiledev.handyhub.databinding.ContentAuthCardBinding
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.emailCheckState.collect { resource ->
                val getStartedEmailLayout = getStartedContent.emailInputLayout
                val joinUsEmailLayout = joinUsContent.emailInputLayout
                val getStartedProgress = getStartedContent.emailCheckProgress
                val joinUsProgress = joinUsContent.emailCheckProgress
                val getStartedButton = getStartedContent.buttonContinue
                val joinUsButton = joinUsContent.buttonContinue
                
                when (resource) {
                    is Resource.Success -> {
                        getStartedEmailLayout.helperText = ""
                        joinUsEmailLayout.helperText = ""
                        getStartedProgress.visibility = android.view.View.GONE
                        joinUsProgress.visibility = android.view.View.GONE
                        getStartedButton.isEnabled = true
                        joinUsButton.isEnabled = true
                        
                        if (resource.data == true) {
                            val getStartedState = authViewModel.getStartedState.value
                            val joinUsState = authViewModel.joinUsState.value
                            
                            if (getStartedState.isEmailMode && getStartedState.email.isNotEmpty()) {
                                authViewModel.updateGetStartedState { it.copy(isOtpMode = true) }
                            }
                            if (joinUsState.isEmailMode && joinUsState.email.isNotEmpty()) {
                                authViewModel.updateJoinUsState { it.copy(isOtpMode = true) }
                            }
                        } else {
                            getStartedEmailLayout.error = "Email not found"
                            joinUsEmailLayout.error = "Email not found"
                        }
                    }
                    is Resource.Error -> {
                        getStartedEmailLayout.helperText = ""
                        joinUsEmailLayout.helperText = ""
                        getStartedEmailLayout.error = "Error checking email"
                        joinUsEmailLayout.error = "Error checking email"
                        getStartedProgress.visibility = android.view.View.GONE
                        joinUsProgress.visibility = android.view.View.GONE
                        getStartedButton.isEnabled = true
                        joinUsButton.isEnabled = true
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_SHORT).show()
    }
}

