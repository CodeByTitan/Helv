package ca.unb.mobiledev.handyhub.messages.data.repository

import ca.unb.mobiledev.handyhub.messages.domain.model.ChatMessage
import ca.unb.mobiledev.handyhub.messages.domain.model.Conversation
import ca.unb.mobiledev.handyhub.messages.domain.model.ParticipantInfo
import ca.unb.mobiledev.handyhub.messages.domain.model.TopHelper
import ca.unb.mobiledev.handyhub.messages.domain.repository.MessagesRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessagesRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MessagesRepository {

    companion object {
        private const val MESSAGES_COLLECTION = "messages"
        private const val MESSAGES_SUBCOLLECTION = "messages"
    }

    /**
     * Generate a consistent conversation ID from two user IDs
     * Always sorted alphabetically to ensure same ID regardless of who initiates
     */
    private fun getConversationId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    override fun getConversationsFlow(userId: String): Flow<Resource<List<Conversation>>> = callbackFlow {
        trySend(Resource.Loading())

        val listenerRegistration = firestore.collection(MESSAGES_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load conversations"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        try {
                            Conversation(
                                id = doc.id,
                                participants = doc.get("participants") as? List<String> ?: emptyList(),
                                participantDetails = (doc.get("participantDetails") as? Map<String, Map<String, Any>>)?.mapValues { entry ->
                                    val details = entry.value
                                    ParticipantInfo(
                                        name = details["name"] as? String ?: "",
                                        imageUrl = details["imageUrl"] as? String ?: "",
                                        role = details["role"] as? String ?: "",
                                        category = details["category"] as? String ?: ""
                                    )
                                } ?: emptyMap(),
                                lastMessage = doc.getString("lastMessage") ?: "",
                                lastMessageTime = doc.getTimestamp("lastMessageTime") ?: Timestamp.now(),
                                lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                                unreadCount = doc.get("unreadCount") as? Map<String, Int> ?: emptyMap(),
                                isMuted = doc.get("isMuted") as? Map<String, Boolean> ?: emptyMap(),
                                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                                updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(conversations))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String,
        otherUserDetails: Map<String, Any>
    ): Resource<String> {
        return try {
            val conversationId = getConversationId(currentUserId, otherUserId)
            val conversationRef = firestore.collection(MESSAGES_COLLECTION).document(conversationId)

            val doc = conversationRef.get().await()

            if (!doc.exists()) {
                // Create new conversation
                val conversation = hashMapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "participantDetails" to otherUserDetails,
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSenderId" to "",
                    "unreadCount" to mapOf(currentUserId to 0, otherUserId to 0),
                    "isMuted" to mapOf(currentUserId to false, otherUserId to false),
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
                conversationRef.set(conversation).await()
            }

            Resource.Success(conversationId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create conversation")
        }
    }

    override suspend fun muteConversation(
        conversationId: String,
        userId: String,
        isMuted: Boolean
    ): Resource<Unit> {
        return try {
            firestore.collection(MESSAGES_COLLECTION)
                .document(conversationId)
                .update("isMuted.$userId", isMuted)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mute conversation")
        }
    }

    override suspend fun deleteConversation(
        conversationId: String,
        userId: String
    ): Resource<Unit> {
        return try {
            // In a real app, you might want to soft-delete or archive instead
            // For now, we'll just delete the conversation document
            firestore.collection(MESSAGES_COLLECTION)
                .document(conversationId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete conversation")
        }
    }

    override fun getMessagesFlow(conversationId: String): Flow<Resource<List<ChatMessage>>> = callbackFlow {
        trySend(Resource.Loading())

        val listenerRegistration = firestore.collection(MESSAGES_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load messages"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChatMessage(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: "",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                type = doc.getString("type") ?: "text",
                                isRead = doc.getBoolean("isRead") ?: false,
                                readAt = doc.getTimestamp("readAt")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(messages))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String
    ): Resource<Unit> {
        return try {
            val messageData = hashMapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to Timestamp.now(),
                "type" to "text",
                "isRead" to false
            )

            // Add message to subcollection
            firestore.collection(MESSAGES_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_SUBCOLLECTION)
                .add(messageData)
                .await()

            // Update conversation metadata
            val conversationRef = firestore.collection(MESSAGES_COLLECTION).document(conversationId)
            val conversation = conversationRef.get().await()
            val participants = conversation.get("participants") as? List<String> ?: emptyList()
            val otherUserId = participants.firstOrNull { it != senderId } ?: ""

            conversationRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSenderId" to senderId,
                    "unreadCount.$otherUserId" to FieldValue.increment(1),
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    override suspend fun markMessagesAsRead(
        conversationId: String,
        userId: String
    ): Resource<Unit> {
        return try {
            // Reset unread count for this user
            firestore.collection(MESSAGES_COLLECTION)
                .document(conversationId)
                .update("unreadCount.$userId", 0)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark messages as read")
        }
    }

    override suspend fun getTopHelpers(): Resource<List<TopHelper>> {
        return try {
            val helpers = listOf(
                TopHelper(
                    id = "1",
                    fullName = "Sarah Johnson",
                    category = "Professional House Cleaning",
                    location = "Downtown",
                    distance = "1.2 km",
                    rating = 4.9,
                    pricePerHour = 45,
                    imageUrl = "https://i.pravatar.cc/150?img=1",
                    isAvailable = true
                ),
                TopHelper(
                    id = "2",
                    fullName = "Michael Chen",
                    category = "Appliance Repair Specialist",
                    location = "North End",
                    distance = "2.5 km",
                    rating = 4.8,
                    pricePerHour = 60,
                    imageUrl = "https://i.pravatar.cc/150?img=12",
                    isAvailable = true
                ),
                TopHelper(
                    id = "3",
                    fullName = "Emily Rodriguez",
                    category = "Spa & Beauty Treatments",
                    location = "Midtown",
                    distance = "0.8 km",
                    rating = 5.0,
                    pricePerSession = 55,
                    imageUrl = "https://i.pravatar.cc/150?img=5",
                    isAvailable = false
                ),
                TopHelper(
                    id = "4",
                    fullName = "James Wilson",
                    category = "Personal Fitness Trainer",
                    location = "Westside",
                    distance = "1.5 km",
                    rating = 4.7,
                    pricePerHour = 50,
                    imageUrl = "https://i.pravatar.cc/150?img=13",
                    isAvailable = true
                )
            )

            Resource.Success(helpers)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred while loading top helpers")
        }
    }
}
