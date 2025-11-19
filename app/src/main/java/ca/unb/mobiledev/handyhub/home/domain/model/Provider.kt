package ca.unb.mobiledev.handyhub.home.domain.model

data class Provider(
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


