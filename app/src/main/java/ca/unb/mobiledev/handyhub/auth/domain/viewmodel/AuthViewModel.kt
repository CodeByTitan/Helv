package ca.unb.mobiledev.handyhub.auth.domain.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.auth.domain.model.User
import ca.unb.mobiledev.handyhub.auth.domain.repository.AuthRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()


    private val _onboardingStatus = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val onboardingStatus: StateFlow<Resource<Boolean>> = _onboardingStatus.asStateFlow()
    
    private val _otpState = MutableStateFlow<Resource<String>>(Resource.Loading())
    val otpState: StateFlow<Resource<String>> = _otpState.asStateFlow()
    
    private val _emailCheckState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val emailCheckState: StateFlow<Resource<Boolean>> = _emailCheckState.asStateFlow()
    
    private val _phoneCheckState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val phoneCheckState: StateFlow<Resource<Boolean>> = _phoneCheckState.asStateFlow()
    
    data class AuthFlowState(
        val isGetStartedCard: Boolean = true,
        val isLoginMode: Boolean = false,
        val isEmailMode: Boolean = false,
        val isOtpMode: Boolean = false,
        val phoneNumber: String = "",
        val email: String = "",
        val isReturningUser: Boolean = false
    )
    
    private val _getStartedState = MutableStateFlow(AuthFlowState(isGetStartedCard = true))
    val getStartedState: StateFlow<AuthFlowState> = _getStartedState.asStateFlow()
    
    private val _joinUsState = MutableStateFlow(AuthFlowState(isGetStartedCard = false))
    val joinUsState: StateFlow<AuthFlowState> = _joinUsState.asStateFlow()
    
    


    fun sendOtp(phone: String, activity: Activity) {
        viewModelScope.launch {
            authRepository.sendOtp(phone, activity).collect { resource ->
                _otpState.value = resource
            }
        }
    }

    fun signInWithPhone(phone: String, otp: String) {
        viewModelScope.launch {
            authRepository.signInWithPhone(phone, otp).collect {
                _authState.value = it
            }
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            authRepository.checkOnboardingStatus().collect {
                _onboardingStatus.value = it
            }
        }
    }

    fun checkEmailExists(email: String) {
        viewModelScope.launch {
            authRepository.checkEmailExists(email).collect { resource ->
                _emailCheckState.value = resource
            }
        }
    }
    
    fun checkPhoneExists(phone: String) {
        viewModelScope.launch {
            authRepository.checkPhoneExists(phone).collect { resource ->
                _phoneCheckState.value = resource
            }
        }
    }
    
    fun updateGetStartedState(update: (AuthFlowState) -> AuthFlowState) {
        _getStartedState.value = update(_getStartedState.value)
    }
    
    fun updateJoinUsState(update: (AuthFlowState) -> AuthFlowState) {
        _joinUsState.value = update(_joinUsState.value)
    }
    
    fun resetGetStartedState() {
        _getStartedState.value = AuthFlowState(isGetStartedCard = true)
    }
    
    fun resetJoinUsState() {
        _joinUsState.value = AuthFlowState(isGetStartedCard = false)
    }
    
    private val _updateDetailsState = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val updateDetailsState: StateFlow<Resource<Unit>> = _updateDetailsState.asStateFlow()
    
    private val _otpResendTimer = MutableStateFlow(0)
    val otpResendTimer: StateFlow<Int> = _otpResendTimer.asStateFlow()
    
    private var timerJob: kotlinx.coroutines.Job? = null
    
    fun startOtpResendTimer() {
        timerJob?.cancel()
        _otpResendTimer.value = 60
        
        timerJob = viewModelScope.launch {
            while (_otpResendTimer.value > 0) {
                kotlinx.coroutines.delay(1000)
                _otpResendTimer.value = _otpResendTimer.value - 1
            }
        }
    }
    
    fun canResendOtp(): Boolean {
        return _otpResendTimer.value == 0
    }
    
    fun updateUserDetailsAndCompleteOnboarding(name: String, email: String, dob: String) {
        viewModelScope.launch {
            authRepository.updateUserDetailsAndCompleteOnboarding(name, email, dob).collect { resource ->
                _updateDetailsState.value = resource
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}



