package ca.unb.mobiledev.handyhub.home.domain.model

data class Booking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val workerId: String = "",
    val serviceCategory: String = "",
    val serviceName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "pending",
    val totalAmount: Int = 0,
    val notes: String = ""
)

