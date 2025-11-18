package ca.unb.mobiledev.handyhub.messages.domain.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(), // [userId1, userId2]
    val participantDetails: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Timestamp = Timestamp.now(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(),
    val isMuted: Map<String, Boolean> = emptyMap(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class ParticipantInfo(
    val name: String = "",
    val imageUrl: String = "",
    val role: String = "", // "client" or "worker"
    val category: String = "" // Only for workers
)

