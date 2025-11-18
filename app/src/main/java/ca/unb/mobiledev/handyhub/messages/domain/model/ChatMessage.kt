package ca.unb.mobiledev.handyhub.messages.domain.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text", // "text", "audio", "image"
    val isRead: Boolean = false,
    val readAt: Timestamp? = null
) {
    // Helper to determine if message was sent by current user
    fun isSentByUser(currentUserId: String): Boolean = senderId == currentUserId
}
