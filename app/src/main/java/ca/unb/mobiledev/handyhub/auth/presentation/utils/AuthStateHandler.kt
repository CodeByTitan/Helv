package ca.unb.mobiledev.handyhub.auth.presentation.utils

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel

class AuthStateHandler(
    private val authViewModel: AuthViewModel,
    private val isGetStartedCard: Boolean
) {
    
    val state: AuthViewModel.AuthFlowState
        get() = if (isGetStartedCard) {
            authViewModel.getStartedState.value
        } else {
            authViewModel.joinUsState.value
        }
    
    fun toggleLoginMode() {
        updateState { it.copy(isLoginMode = !it.isLoginMode) }
    }
    
    fun toggleEmailMode() {
        updateState { it.copy(isEmailMode = !it.isEmailMode) }
    }
    
    fun enterOtpMode(phoneOrEmail: String) {
        if (state.isEmailMode) {
            updateState { it.copy(isOtpMode = true, email = phoneOrEmail) }
        } else {
            updateState { it.copy(isOtpMode = true, phoneNumber = phoneOrEmail) }
        }
    }
    
    fun reset() {
        if (isGetStartedCard) {
            authViewModel.resetGetStartedState()
        } else {
            authViewModel.resetJoinUsState()
        }
    }
    
    fun setEmail(email: String) {
        updateState { it.copy(email = email) }
    }
    
    fun updateInputVisibility(
        phoneLayout: TextInputLayout,
        emailLayout: TextInputLayout,
        otpLayout: TextInputLayout,
        useEmailToggle: android.widget.TextView,
        toggleText: android.widget.TextView
    ) {
        
        when {
            state.isOtpMode -> {
                phoneLayout.visibility = View.GONE
                emailLayout.visibility = View.GONE
                otpLayout.visibility = View.VISIBLE
                useEmailToggle.visibility = View.GONE
                toggleText.visibility = View.VISIBLE
            }
            state.isEmailMode -> {
                phoneLayout.visibility = View.GONE
                emailLayout.visibility = View.VISIBLE
                otpLayout.visibility = View.GONE
                useEmailToggle.visibility = View.VISIBLE
                toggleText.visibility = View.VISIBLE
            }
            else -> {
                phoneLayout.visibility = View.VISIBLE
                emailLayout.visibility = View.GONE
                otpLayout.visibility = View.GONE
                useEmailToggle.visibility = if (state.isLoginMode) View.VISIBLE else View.GONE
                toggleText.visibility = View.VISIBLE
            }
        }
        
    }
    
    fun getToggleText(timerSeconds: Int = 0): String {
        return when {
            state.isOtpMode && timerSeconds > 0 -> "Resend OTP in ${timerSeconds}s"
            state.isOtpMode -> "Resend OTP"
            state.isLoginMode -> "Don't have an account?"
            else -> "Already have an account?"
        }
    }
    
    fun getEmailToggleText(): String {
        return if (state.isEmailMode) "Having trouble signing in?" else "Use email instead"
    }
    
    fun getTitleText(cardTitle: String, isReturningUser: Boolean = false): String {
        return when {
            state.isOtpMode && isReturningUser -> "Welcome Back Homie"
            state.isOtpMode -> "Verify OTP"
            state.isLoginMode && isGetStartedCard -> "Get Back In"
            state.isLoginMode && !isGetStartedCard -> "Welcome Back."
            else -> cardTitle
        }
    }
    
    fun getSubtitleText(defaultSubtitle: String, phoneNumber: String = "", isReturningUser: Boolean = false): String {
        return when {
            state.isOtpMode && phoneNumber.isNotEmpty() -> 
                "We have sent you a private pin on the phone number $phoneNumber"
            state.isLoginMode && isGetStartedCard -> 
                "We were looking for you. Login and get chores done."
            state.isLoginMode && !isGetStartedCard -> 
                "We can't wait to help you help others"
            else -> defaultSubtitle
        }
    }
    
    private fun updateState(update: (AuthViewModel.AuthFlowState) -> AuthViewModel.AuthFlowState) {
        if (isGetStartedCard) {
            authViewModel.updateGetStartedState(update)
        } else {
            authViewModel.updateJoinUsState(update)
        }
    }
}

