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
    private var hasClickedExploreButton = false
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeConversations()
        observeTopHelpers()
    }
    
    private fun setupClickListeners() {
        binding.buttonExploreHelpers.setOnClickListener {
            animateButtonToTitle()
        }
        
        binding.buttonChatIcon.setOnClickListener {
            scrollToMessages()
        }
    }
    
    private fun scrollToMessages() {
        val title = binding.textViewTopHelpersTitle
        val scrollView = binding.root as androidx.core.widget.NestedScrollView
        
        val titleAnimator = android.animation.ObjectAnimator.ofFloat(title, "alpha", 1f, 0f)
        titleAnimator.duration = 300
        titleAnimator.start()
        
        val recyclerViewAnimator = android.animation.ObjectAnimator.ofFloat(binding.recyclerViewTopHelpers, "alpha", 1f, 0f)
        recyclerViewAnimator.duration = 300
        recyclerViewAnimator.start()
        
        scrollView.postDelayed({
            binding.recyclerViewTopHelpers.visibility = View.GONE
            binding.helpersHeaderContainer.visibility = View.GONE
            
            binding.recyclerViewMessages.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.textViewTitle.visibility = View.VISIBLE
            binding.buttonExploreHelpers.visibility = View.VISIBLE
            binding.imageViewEcotourism.visibility = View.VISIBLE
            
            binding.textViewTitle.alpha = 0f
            binding.recyclerViewMessages.alpha = 0f
            binding.emptyStateContainer.alpha = 0f
            binding.buttonExploreHelpers.alpha = 0f
            binding.imageViewEcotourism.alpha = 0f
            
            val titleFadeIn = android.animation.ObjectAnimator.ofFloat(binding.textViewTitle, "alpha", 0f, 1f)
            titleFadeIn.duration = 400
            titleFadeIn.start()
            
            val messagesFadeIn = android.animation.ObjectAnimator.ofFloat(binding.recyclerViewMessages, "alpha", 0f, 1f)
            messagesFadeIn.duration = 400
            messagesFadeIn.start()
            
            val emptyStateFadeIn = android.animation.ObjectAnimator.ofFloat(binding.emptyStateContainer, "alpha", 0f, 1f)
            emptyStateFadeIn.duration = 400
            emptyStateFadeIn.start()
            
            val buttonFadeIn = android.animation.ObjectAnimator.ofFloat(binding.buttonExploreHelpers, "alpha", 0f, 1f)
            buttonFadeIn.duration = 400
            buttonFadeIn.start()
            
            val imageFadeIn = android.animation.ObjectAnimator.ofFloat(binding.imageViewEcotourism, "alpha", 0f, 1f)
            imageFadeIn.duration = 400
            imageFadeIn.start()
            
            scrollView.smoothScrollTo(0, 0)
        }, 300)
    }
    
    private fun animateButtonToTitle() {
        hasClickedExploreButton = true
        val button = binding.buttonExploreHelpers
        val title = binding.textViewTopHelpersTitle
        val scrollView = binding.root as androidx.core.widget.NestedScrollView
        
        viewModel.refreshTopHelpers()
        
        title.alpha = 0f
        title.visibility = View.VISIBLE
        binding.recyclerViewTopHelpers.visibility = View.VISIBLE
        
        val buttonAnimator = android.animation.ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)
        buttonAnimator.duration = 300
        buttonAnimator.start()
        
        val imageAnimator = android.animation.ObjectAnimator.ofFloat(binding.imageViewEcotourism, "alpha", 1f, 0f)
        imageAnimator.duration = 300
        imageAnimator.start()
        
        scrollView.postDelayed({
            binding.recyclerViewMessages.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.GONE
            binding.textViewTitle.visibility = View.GONE
            binding.imageViewEcotourism.visibility = View.GONE
            button.visibility = View.GONE
            
            binding.helpersHeaderContainer.visibility = View.VISIBLE
            val titleAnimator = android.animation.ObjectAnimator.ofFloat(title, "alpha", 0f, 1f)
            titleAnimator.duration = 400
            titleAnimator.start()
            
            scrollView.smoothScrollTo(0, 0)
        }, 300)
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
        
        topHelperAdapter = TopHelperAdapter(
            onItemClick = { helper ->
                Toast.makeText(
                    requireContext(),
                    "View profile: ${helper.fullName}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onMessageClick = { helper ->
                startConversationWithHelper(helper)
            }
        )
        
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
                        showEmptyState(true)
                    }
                }
            }
        }
    }
    
    private fun observeTopHelpers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topHelpers.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        android.util.Log.d("MessagesFragment", "Loading top helpers...")
                    }
                    is Resource.Success -> {
                        val helpers = resource.data ?: emptyList()
                        android.util.Log.d("MessagesFragment", "Loaded ${helpers.size} top helpers")
                        topHelperAdapter.submitList(helpers)
                        if (hasClickedExploreButton) {
                            binding.helpersHeaderContainer.visibility = if (helpers.isNotEmpty()) View.VISIBLE else View.GONE
                            binding.recyclerViewTopHelpers.visibility = if (helpers.isNotEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                    is Resource.Error -> {
                            android.util.Log.e("MessagesFragment", "Error loading top helpers: ${resource.message}")
                                if (hasClickedExploreButton) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error loading helpers: ${resource.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                binding.helpersHeaderContainer.visibility = View.GONE
                                binding.recyclerViewTopHelpers.visibility = View.GONE
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
    
    private fun startConversationWithHelper(helper: ca.unb.mobiledev.handyhub.messages.domain.model.TopHelper) {
        viewLifecycleOwner.lifecycleScope.launch {
            val otherUserDetails = mapOf(
                "name" to helper.fullName,
                "category" to helper.category,
                "imageUrl" to helper.imageUrl
            )
            
            val result = viewModel.startConversation(helper.id, otherUserDetails)
            when (result) {
                is Resource.Success -> {
                    val conversationId = result.data ?: return@launch
                    val firstName = helper.fullName.split(" ").firstOrNull() ?: helper.fullName
                    val bundle = Bundle().apply {
                        putString("conversationId", conversationId)
                        putString("workerName", firstName)
                        putString("workerCategory", helper.category)
                        putString("workerImageUrl", helper.imageUrl)
                    }
                    findNavController().navigate(
                        R.id.action_messagesFragment_to_chatFragment,
                        bundle
                    )
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: "Failed to start conversation",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {
                    // Show loading if needed
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
