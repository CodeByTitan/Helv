package ca.unb.mobiledev.handyhub.messages.domain.repository

import ca.unb.mobiledev.handyhub.messages.domain.model.ChatMessage
import ca.unb.mobiledev.handyhub.messages.domain.model.Conversation
import ca.unb.mobiledev.handyhub.messages.domain.model.TopHelper
import ca.unb.mobiledev.handyhub.util.Resource
import kotlinx.coroutines.flow.Flow

interface MessagesRepository {
    // Conversations
    fun getConversationsFlow(userId: String): Flow<Resource<List<Conversation>>>
    suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String, otherUserDetails: Map<String, Any>): Resource<String>
    suspend fun muteConversation(conversationId: String, userId: String, isMuted: Boolean): Resource<Unit>
    suspend fun deleteConversation(conversationId: String, userId: String): Resource<Unit>
    
    // Messages
    fun getMessagesFlow(conversationId: String): Flow<Resource<List<ChatMessage>>>
    suspend fun sendMessage(conversationId: String, senderId: String, text: String): Resource<Unit>
    suspend fun markMessagesAsRead(conversationId: String, userId: String): Resource<Unit>
    
    // Top Helpers (dummy data for now)
    suspend fun getTopHelpers(): Resource<List<TopHelper>>
}

