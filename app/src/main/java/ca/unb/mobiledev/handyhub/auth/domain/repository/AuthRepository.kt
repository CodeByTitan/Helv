package ca.unb.mobiledev.handyhub.auth.domain.repository

import ca.unb.mobiledev.handyhub.auth.domain.model.User
import ca.unb.mobiledev.handyhub.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun sendOtp(phone:String, activity: android.app.Activity): Flow<Resource<String>>
    fun signInWithPhone(phone:String, otp:String): Flow<Resource<User>>
    fun checkOnboardingStatus(): Flow<Resource<Boolean>>
    fun checkEmailExists(email: String): Flow<Resource<Boolean>>
    fun checkPhoneExists(phone: String): Flow<Resource<Boolean>>
    fun updateUserDetailsAndCompleteOnboarding(name: String, email: String, dob: String): Flow<Resource<Unit>>
}


