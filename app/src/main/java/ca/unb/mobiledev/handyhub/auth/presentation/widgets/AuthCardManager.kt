package ca.unb.mobiledev.handyhub.auth.presentation.widgets

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel
import ca.unb.mobiledev.handyhub.auth.presentation.utils.AuthInputValidator
import ca.unb.mobiledev.handyhub.auth.presentation.utils.AuthStateHandler
import ca.unb.mobiledev.handyhub.databinding.ContentAuthCardBinding

/**
 * Manages all card logic including input handling, validation, and interactions
 */
class AuthCardManager(
    private val fragment: Fragment,
    private val authViewModel: AuthViewModel,
    private val isGetStarted: Boolean
) {
    private val handler = AuthStateHandler(authViewModel, isGetStarted)
    private lateinit var content: ContentAuthCardBinding
    
    fun getContent(): ContentAuthCardBinding = content
    
    
    fun inflateContent(container: ViewGroup): ContentAuthCardBinding {
        content = ContentAuthCardBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            true
        )
        setupListeners()
        return content
    }
    
    fun getHandler() = handler
    
    private fun setupListeners() {
        content.apply {
            phoneInput.addTextChangedListener(InputWatchers.createPhoneWatcher {
                phoneInputLayout.error = null
            })
            
            emailInput.addTextChangedListener(InputWatchers.createEmailWatcher(this))
            otpInput.addTextChangedListener(InputWatchers.createOtpWatcher(this))
            
            toggleText.setOnClickListener { handleToggleClick() }
            useEmailToggle.setOnClickListener { handleEmailToggleClick() }
            buttonContinue.setOnClickListener { handleContinueClick() }
        }
    }
    
    private fun handleToggleClick() {
        val state = if (isGetStarted) authViewModel.getStartedState.value else authViewModel.joinUsState.value
        
        if (state.isOtpMode) {
            resendOtp()
        } else {
            val wasInLoginMode = state.isLoginMode
            handler.toggleLoginMode()
            
            if (wasInLoginMode && state.isEmailMode) {
                handler.toggleEmailMode()
                content.emailInput.text?.clear()
                content.emailInputLayout.error = null
            }
        }
    }
    
    private fun handleEmailToggleClick() {
        handler.toggleEmailMode()
        if (!handler.state.isEmailMode) {
            content.emailInput.text?.clear()
            content.emailInputLayout.error = null
        } else {
            content.phoneInput.text?.clear()
            content.phoneInputLayout.error = null
        }
    }
    
    private fun handleContinueClick() {
        val state = if (isGetStarted) authViewModel.getStartedState.value else authViewModel.joinUsState.value
        
        when {
            state.isOtpMode -> verifyOtp()
            state.isEmailMode -> handleEmailContinue()
            else -> handlePhoneContinue()
        }
    }
    
    private fun handlePhoneContinue() {
        val phone = content.phoneInput.text.toString()
        
        if (!AuthInputValidator.isValidPhone(phone)) {
            content.phoneInputLayout.error = "Phone number must be 10 digits"
            return
        }
        
        sendOtp(AuthInputValidator.formatPhoneNumber(phone))
    }
    
    private fun handleEmailContinue() {
        val email = content.emailInput.text.toString()
        
        if (!AuthInputValidator.isValidEmail(email)) {
            showToast("Please enter a valid email")
            return
        }
        
        content.emailInputLayout.helperText = "Checking for matching account..."
        content.emailCheckProgress.visibility = android.view.View.VISIBLE
        content.buttonContinue.isEnabled = false
        
        authViewModel.checkEmailExists(email)
    }
    
    private fun sendOtp(phoneNumber: String) {
        handler.enterOtpMode(phoneNumber)
        authViewModel.sendOtp(phoneNumber, fragment.requireActivity())
    }
    
    private fun resendOtp() {
        val state = if (isGetStarted) authViewModel.getStartedState.value else authViewModel.joinUsState.value
        sendOtp(state.phoneNumber)
    }
    
    private fun verifyOtp() {
        val state = if (isGetStarted) authViewModel.getStartedState.value else authViewModel.joinUsState.value
        val otp = content.otpInput.text.toString()
        
        if (!AuthInputValidator.isValidOtp(otp)) {
            showToast("Please enter valid 6-digit OTP")
            return
        }
        
        authViewModel.signInWithPhone(state.phoneNumber, otp)
    }
    
    fun updateUI(state: AuthViewModel.AuthFlowState, card: AuthCardView, defaultTitle: String, defaultSubtitle: String) {
        card.setExpandedTitle(handler.getTitleText(defaultTitle))
        card.setSubtitle(handler.getSubtitleText(defaultSubtitle, state.phoneNumber))
        
        content.toggleText.text = handler.getToggleText()
        content.useEmailToggle.text = handler.getEmailToggleText()
        
        handler.updateInputVisibility(content.phoneInputLayout, content.emailInputLayout, content.otpInputLayout, content.useEmailToggle, content.toggleText)
        
        card.adjustHeightForContent()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_SHORT).show()
    }
}

