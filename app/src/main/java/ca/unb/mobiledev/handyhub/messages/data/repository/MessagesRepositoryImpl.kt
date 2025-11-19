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
            android.util.Log.d("MessagesRepository", "Fetching top helpers from Firebase")
            
            val snapshot = try {
                firestore.collection("workers")
                    .whereEqualTo("is_active", true)
                    .orderBy("rating", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    android.util.Log.w("MessagesRepository", "Index missing, trying without orderBy")
                    firestore.collection("workers")
                        .whereEqualTo("is_active", true)
                        .limit(5)
                        .get()
                        .await()
                } else {
                    throw e
                }
            }
            
            android.util.Log.d("MessagesRepository", "Found ${snapshot.documents.size} workers")
            
            val helpers = snapshot.documents.mapNotNull { document ->
                try {
                    val name = document.getString("name") ?: return@mapNotNull null
                    val category = document.getString("category") ?: ""
                    val city = document.getString("city") ?: ""
                    val state = document.getString("state") ?: ""
                    val location = if (city.isNotEmpty() && state.isNotEmpty()) {
                        "$city, $state"
                    } else if (city.isNotEmpty()) {
                        city
                    } else {
                        "Location not specified"
                    }
                    
                    val rating = document.getDouble("rating")?.toDouble() ?: 0.0
                    val hourlyRate = document.getLong("hourly_rate")?.toInt()
                    val profilePicture = document.getString("profile_picture") ?: ""
                    val availabilityStatus = document.getString("availability_status") ?: "unavailable"
                    val isAvailable = availabilityStatus.lowercase() == "available"
                    
                    TopHelper(
                        id = document.id,
                        fullName = name,
                        category = category,
                        location = location,
                        distance = "N/A",
                        rating = rating,
                        pricePerHour = hourlyRate,
                        imageUrl = profilePicture,
                        isAvailable = isAvailable
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MessagesRepository", "Error parsing worker document: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("MessagesRepository", "Successfully parsed ${helpers.size} helpers")
            Resource.Success(helpers.sortedByDescending { it.rating })
        } catch (e: Exception) {
            android.util.Log.e("MessagesRepository", "Error loading top helpers: ${e.message}", e)
            Resource.Error(e.message ?: "An error occurred while loading top helpers")
        }
    }
}
