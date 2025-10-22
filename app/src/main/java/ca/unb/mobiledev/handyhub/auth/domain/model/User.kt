package ca.unb.mobiledev.handyhub.auth.domain.model

data class User(
    val uid: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val onboardingCompleted: Boolean = false
)


