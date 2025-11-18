package ca.unb.mobiledev.handyhub.messages.domain.model

data class Worker(
    val id: String,
    val fullName: String,
    val category: String,
    val imageUrl: String,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false
)

