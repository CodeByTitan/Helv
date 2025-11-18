package ca.unb.mobiledev.handyhub.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.FragmentMessagesBinding
import ca.unb.mobiledev.handyhub.messages.domain.model.Conversation
import ca.unb.mobiledev.handyhub.messages.domain.viewmodel.MessagesViewModel
import ca.unb.mobiledev.handyhub.messages.presentation.adapter.MessageAdapter
import ca.unb.mobiledev.handyhub.messages.presentation.adapter.TopHelperAdapter
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessagesFragment : Fragment() {
    
    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MessagesViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var topHelperAdapter: TopHelperAdapter
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeConversations()
        observeTopHelpers()
    }
    
    private fun setupRecyclerViews() {
        messageAdapter = MessageAdapter(
            onItemClick = { conversation ->
                navigateToChat(conversation)
            },
            onMuteChat = { conversation ->
                toggleMuteConversation(conversation)
            },
            onDeleteChat = { conversation ->
                showDeleteConfirmation(conversation)
            }
        )
        
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
        
        topHelperAdapter = TopHelperAdapter { helper ->
            Toast.makeText(
                requireContext(),
                "View profile: ${helper.fullName}",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        binding.recyclerViewTopHelpers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topHelperAdapter
        }
    }
    
    private fun navigateToChat(conversation: Conversation) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return
        val otherUser = conversation.participantDetails[otherUserId] ?: return
        
        val firstName = otherUser.name.split(" ").firstOrNull() ?: otherUser.name
        val bundle = Bundle().apply {
            putString("conversationId", conversation.id)
            putString("workerName", firstName)
            putString("workerCategory", otherUser.category.ifEmpty { "Worker" })
            putString("workerImageUrl", otherUser.imageUrl)
        }
        findNavController().navigate(
            R.id.action_messagesFragment_to_chatFragment,
            bundle
        )
    }
    
    private fun observeConversations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.conversations.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val conversations = resource.data ?: emptyList()
                        if (conversations.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            messageAdapter.submitList(conversations)
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            resource.message ?: "Failed to load conversations",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun observeTopHelpers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topHelpers.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val helpers = resource.data ?: emptyList()
                        topHelperAdapter.submitList(helpers)
                        binding.textViewTopHelpersTitle.visibility = if (helpers.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    is Resource.Error -> {
                        binding.textViewTopHelpersTitle.visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewMessages.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewMessages.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showDeleteConfirmation(conversation: Conversation) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return
        val otherUser = conversation.participantDetails[otherUserId] ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this conversation with ${otherUser.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteConversation(conversation)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleMuteConversation(conversation: Conversation) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val isMuted = conversation.isMuted[currentUserId] ?: false
        
        viewModel.muteConversation(conversation.id, !isMuted)
        
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return
        val otherUser = conversation.participantDetails[otherUserId] ?: return
        
        val message = if (!isMuted) {
            "Muted ${otherUser.name}"
        } else {
            "Unmuted ${otherUser.name}"
        }
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteConversation(conversation: Conversation) {
        viewModel.deleteConversation(conversation.id)
        
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return
        val otherUser = conversation.participantDetails[otherUserId] ?: return
        
        Toast.makeText(
            requireContext(),
            "Chat with ${otherUser.name} deleted",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
