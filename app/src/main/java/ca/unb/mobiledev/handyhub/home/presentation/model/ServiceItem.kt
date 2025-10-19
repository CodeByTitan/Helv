package ca.unb.mobiledev.handyhub.home.presentation.model

data class ServiceItem(
    val name: String,
    val price: String,
    val imageRes: Int,
    val isPriceHighlighted: Boolean = true
)
