package ca.unb.mobiledev.handyhub.messages.presentation.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ItemMessageBinding
import ca.unb.mobiledev.handyhub.messages.domain.model.Conversation
import ca.unb.mobiledev.handyhub.util.toRelativeTime
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(
    private val onItemClick: (Conversation) -> Unit,
    private val onMuteChat: (Conversation) -> Unit,
    private val onDeleteChat: (Conversation) -> Unit
) : ListAdapter<Conversation, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: Conversation) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            
            // Get the other user's details
            val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: return
            val otherUser = conversation.participantDetails[otherUserId] ?: return
            
            binding.apply {
                val firstName = otherUser.name.split(" ").firstOrNull() ?: otherUser.name
                textViewWorkerName.text = firstName
                textViewWorkerCategory.text = "• ${otherUser.category.ifEmpty { "Worker" }}"
                
                textViewLastMessage.text = conversation.lastMessage.ifEmpty { "No messages yet" }
                textViewMessageTime.text = conversation.lastMessageTime.toRelativeTime()
                
                val unreadCount = conversation.unreadCount[currentUserId] ?: 0
                if (unreadCount > 0) {
                    textViewUnreadCount.visibility = View.VISIBLE
                    textViewUnreadCount.text = unreadCount.toString()
                } else {
                    textViewUnreadCount.visibility = View.GONE
                }
                
                Glide.with(root.context)
                    .load(otherUser.imageUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.ic_profile)
                    .into(imageViewWorker)
                
                root.setOnClickListener {
                    onItemClick(conversation)
                }
                
                root.setOnLongClickListener {
                    showActionMenu(it, conversation, currentUserId)
                    true
                }
            }
        }
        
        private fun showActionMenu(view: View, conversation: Conversation, currentUserId: String) {
            val popupView = LayoutInflater.from(view.context).inflate(R.layout.popup_message_actions, null)
            
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            
            popupWindow.elevation = 10f
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            
            val textViewMute = popupView.findViewById<TextView>(R.id.textViewMute)
            val textViewDelete = popupView.findViewById<TextView>(R.id.textViewDelete)
            
            val isMuted = conversation.isMuted[currentUserId] ?: false
            textViewMute.text = if (isMuted) "Unmute" else "Mute"
            
            textViewMute.setOnClickListener {
                onMuteChat(conversation)
                popupWindow.dismiss()
            }
            
            textViewDelete.setOnClickListener {
                onDeleteChat(conversation)
                popupWindow.dismiss()
            }
            
            popupWindow.showAsDropDown(view, 0, -view.height, Gravity.END)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
