package ca.unb.mobiledev.handyhub.messages.domain.model

data class TopHelper(
    val id: String,
    val fullName: String,
    val category: String,
    val location: String,
    val distance: String,
    val rating: Double,
    val pricePerHour: Int? = null,
    val pricePerSession: Int? = null,
    val imageUrl: String,
    val isAvailable: Boolean = true
)

