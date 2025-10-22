package ca.unb.mobiledev.handyhub.auth.domain.repository

import ca.unb.mobiledev.handyhub.auth.domain.model.User
import ca.unb.mobiledev.handyhub.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<Resource<User?>>
    fun createUser(name:String, email:String, phone:String, dob:String, password:String): Flow<Resource<User>>
    fun signInWithEmail(email:String, password:String): Flow<Resource<User>>
    fun sendOtp(phone:String, activity: android.app.Activity): Flow<Resource<String>>
    fun signInWithPhone(phone:String, otp:String): Flow<Resource<User>>
    fun signOut(): Flow<Resource<Unit>>
    fun checkOnboardingStatus(): Flow<Resource<Boolean>>
    fun checkEmailExists(email: String): Flow<Resource<Boolean>>
    fun updateUserDetailsAndCompleteOnboarding(name: String, email: String, dob: String): Flow<Resource<Unit>>
}


