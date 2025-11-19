package ca.unb.mobiledev.handyhub.home.data.repository

import android.util.Log
import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import ca.unb.mobiledev.handyhub.home.domain.repository.WorkersRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WorkersRepository {

    override suspend fun getWorkersByCategory(category: String): Resource<List<Provider>> {
        return try {
            Log.d("WorkersRepository", "Querying for category: '$category'")
            
            val result = firestore.collection("workers")
                .whereEqualTo("category", category)
                .whereEqualTo("is_active", true)
                .get()
                .await()
            
            Log.d("WorkersRepository", "Found ${result.documents.size} workers with category '$category'")
            val providers = parseProviders(result.documents)
            
            Resource.Success(providers)
        } catch (e: Exception) {
            Log.e("WorkersRepository", "Error loading workers: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to load workers: ${e.localizedMessage}")
        }
    }
    
    private fun parseProviders(documents: List<com.google.firebase.firestore.DocumentSnapshot>): List<Provider> {
        return documents.mapNotNull { document ->
            try {
                val name = document.getString("name") ?: return@mapNotNull null
                val workerCategory = document.getString("category") ?: ""
                Log.d("WorkersRepository", "Worker '$name' has category: '$workerCategory'")
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
                
                Provider(
                    id = document.id,
                    fullName = name,
                    category = workerCategory,
                    location = location,
                    distance = "N/A",
                    rating = rating,
                    pricePerHour = hourlyRate,
                    imageUrl = profilePicture,
                    isAvailable = isAvailable
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
