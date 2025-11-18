package ca.unb.mobiledev.handyhub.messages.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.messages.domain.model.ChatMessage
import ca.unb.mobiledev.handyhub.messages.domain.repository.MessagesRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val messages: StateFlow<Resource<List<ChatMessage>>> = _messages.asStateFlow()

    private val _sendMessageState = MutableStateFlow<Resource<Unit>?>(null)
    val sendMessageState: StateFlow<Resource<Unit>?> = _sendMessageState.asStateFlow()

    private var currentConversationId: String? = null

    fun loadMessages(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            messagesRepository.getMessagesFlow(conversationId).collect { resource ->
                _messages.value = resource
            }
        }
        
        // Mark messages as read when opening chat
        markAsRead(conversationId)
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()
            val result = messagesRepository.sendMessage(conversationId, currentUserId, text)
            _sendMessageState.value = result
            
            // Reset state after a delay
            if (result is Resource.Success) {
                kotlinx.coroutines.delay(500)
                _sendMessageState.value = null
            }
        }
    }

    private fun markAsRead(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            messagesRepository.markMessagesAsRead(conversationId, currentUserId)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}

