package ca.unb.mobiledev.handyhub.auth.data.repository

import android.app.Activity
import ca.unb.mobiledev.handyhub.auth.domain.model.User
import ca.unb.mobiledev.handyhub.auth.domain.repository.AuthRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    

    override fun getCurrentUser(): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                emit(Resource.Success(null))
                return@flow
            }
            
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            
            if (doc.exists()) {
                val onboardingStatus = doc.getString("onboarding_status") ?: "ongoing"
                
                val user = User(
                    uid = firebaseUser.uid,
                    name = doc.getString("name"),
                    email = doc.getString("email"),
                    phone = doc.getString("phone"),
                    onboardingCompleted = onboardingStatus == "complete"
                )
                emit(Resource.Success(user))
            } else {
                emit(Resource.Success(null))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get current user"))
        }
    }

    override fun createUser(name: String, email: String, phone: String, dob: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User creation failed")
            
            val userData = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "dob" to dob,
                "onboarding_status" to "complete"
            )
            firestore.collection("users").document(uid).set(userData).await()
            
            val user = User(uid, name, email, phone, true)
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create user"))
        }
    }

    override fun signInWithEmail(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            
            val doc = firestore.collection("users").document(uid).get().await()
            val onboardingStatus = doc.getString("onboarding_status") ?: "ongoing"
            val user = User(
                uid = uid,
                name = doc.getString("name"),
                email = doc.getString("email"),
                phone = doc.getString("phone"),
                onboardingCompleted = onboardingStatus == "complete"
            )
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Login failed"))
        }
    }

    override fun sendOtp(phone: String, activity: Activity): Flow<Resource<String>> = callbackFlow {
        trySend(Resource.Loading())
        
        auth.setLanguageCode("en")
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                storedVerificationId = null
                
                repositoryScope.launch {
                    try {
                        val result = auth.signInWithCredential(credential).await()
                        trySend(Resource.Success("Auto-verification completed"))
                    } catch (e: Exception) {
                        trySend(Resource.Error(e.message ?: "Auto-verification failed"))
                    }
                }
                close()
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                trySend(Resource.Error(e.message ?: "Verification failed"))
                close()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                trySend(Resource.Success("OTP sent successfully"))
                close()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose {
        }
    }

    override fun signInWithPhone(phone: String, otp: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            
            if (storedVerificationId == null) {
                emit(Resource.Error("Please request OTP first"))
                return@flow
            }
            
            val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
            
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            
            
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val statusString = doc.getString("onboarding_status")
                if (statusString == null) {
                    firestore.collection("users").document(uid).update(mapOf(
                        "phone" to phone,
                        "onboarding_status" to "ongoing"
                    )).await()
                }
                val user = User(
                    uid = uid,
                    name = doc.getString("name"),
                    email = doc.getString("email"),
                    phone = doc.getString("phone") ?: phone,
                    onboardingCompleted = (statusString ?: "ongoing") == "complete"
                )
                emit(Resource.Success(user))
            } else {
                val userData = hashMapOf(
                    "phone" to phone,
                    "onboarding_status" to "ongoing"
                )
                firestore.collection("users").document(uid).set(userData).await()
                val user = User(uid, null, null, phone, false)
                emit(Resource.Success(user))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Phone login failed"))
        }
    }

    override fun signOut(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            auth.signOut()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sign out failed"))
        }
    }

    override fun checkOnboardingStatus(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                emit(Resource.Error("No user logged in"))
                return@flow
            }
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val statusString = doc.getString("onboarding_status")
            val isComplete = statusString == "complete"
            emit(Resource.Success(isComplete))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to check onboarding status"))
        }
    }
    
    override fun checkEmailExists(email: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            val exists = !querySnapshot.isEmpty
            emit(Resource.Success(exists))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to check email"))
        }
    }
    
    override fun updateUserDetailsAndCompleteOnboarding(name: String, email: String, dob: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val currentUser = auth.currentUser
            if (currentUser == null) {
                emit(Resource.Error("User not authenticated"))
                return@flow
            }
            
            
            val userUpdates = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "dob" to dob,
                "onboarding_status" to "complete"
            )
            
            firestore.collection("users")
                .document(currentUser.uid)
                .update(userUpdates)
                .await()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update user details"))
        }
    }
}



