package ca.unb.mobiledev.handyhub.messages.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.messages.domain.model.Conversation
import ca.unb.mobiledev.handyhub.messages.domain.model.TopHelper
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
class MessagesViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _conversations = MutableStateFlow<Resource<List<Conversation>>>(Resource.Loading())
    val conversations: StateFlow<Resource<List<Conversation>>> = _conversations.asStateFlow()

    private val _topHelpers = MutableStateFlow<Resource<List<TopHelper>>>(Resource.Loading())
    val topHelpers: StateFlow<Resource<List<TopHelper>>> = _topHelpers.asStateFlow()

    init {
        loadConversations()
        loadTopHelpers()
    }

    private fun loadConversations() {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            messagesRepository.getConversationsFlow(currentUserId).collect { resource ->
                _conversations.value = resource
            }
        }
    }

    private fun loadTopHelpers() {
        viewModelScope.launch {
            _topHelpers.value = Resource.Loading()
            _topHelpers.value = messagesRepository.getTopHelpers()
        }
    }

    fun muteConversation(conversationId: String, isMuted: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            messagesRepository.muteConversation(conversationId, currentUserId, isMuted)
        }
    }

    fun deleteConversation(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            messagesRepository.deleteConversation(conversationId, currentUserId)
        }
    }

    fun refreshConversations() {
        loadConversations()
    }

    fun refreshTopHelpers() {
        loadTopHelpers()
    }
    
    suspend fun startConversation(otherUserId: String, otherUserDetails: Map<String, Any>): Resource<String> {
        val currentUserId = auth.currentUser?.uid ?: return Resource.Error("User not authenticated")
        return messagesRepository.getOrCreateConversation(currentUserId, otherUserId, otherUserDetails)
    }
}
