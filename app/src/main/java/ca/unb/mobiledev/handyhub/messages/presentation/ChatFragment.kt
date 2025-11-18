package ca.unb.mobiledev.handyhub.messages.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.FragmentChatBinding
import ca.unb.mobiledev.handyhub.messages.domain.viewmodel.ChatViewModel
import ca.unb.mobiledev.handyhub.messages.presentation.adapter.ChatAdapter
import ca.unb.mobiledev.handyhub.util.Resource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var conversationId: String? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
        observeMessages()
        observeSendMessageState()
    }
    
    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.GONE
    }
    
    private fun setupHeader() {
        val workerName = arguments?.getString("workerName") ?: "Worker"
        val workerCategory = arguments?.getString("workerCategory") ?: "Service"
        val workerImageUrl = arguments?.getString("workerImageUrl") ?: ""
        conversationId = arguments?.getString("conversationId")
        
        binding.textViewWorkerName.text = workerName
        binding.textViewWorkerCategory.text = workerCategory
        
        Glide.with(this)
            .load(workerImageUrl)
            .placeholder(R.drawable.ic_profile)
            .transform(CircleCrop())
            .into(binding.imageViewWorkerAvatar)
    }
    
    private fun setupRecyclerView() {
        val currentUserId = viewModel.getCurrentUserId() ?: return
        chatAdapter = ChatAdapter(currentUserId)
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }
    
    private fun loadMessages() {
        conversationId?.let { id ->
            viewModel.loadMessages(id)
        }
    }
    
    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Show loading if needed
                    }
                    is Resource.Success -> {
                        val messages = resource.data ?: emptyList()
                        chatAdapter.submitList(messages) {
                            // Scroll to bottom after messages are loaded
                            if (messages.isNotEmpty()) {
                                binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resource.message ?: "Failed to load messages",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun observeSendMessageState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sendMessageState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.buttonSend.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.buttonSend.isEnabled = true
                        binding.editTextMessage.text?.clear()
                    }
                    is Resource.Error -> {
                        binding.buttonSend.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            resource.message ?: "Failed to send message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    null -> {
                        binding.buttonSend.isEnabled = true
                    }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.buttonSend.setOnClickListener {
            sendMessage()
        }
        
        binding.buttonMicrophone.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Voice messages coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun sendMessage() {
        val text = binding.editTextMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        
        conversationId?.let { id ->
            viewModel.sendMessage(id, text)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
    
    private fun showBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.VISIBLE
    }
}
