package ca.unb.mobiledev.handyhub.home.data.repository

import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.home.domain.repository.ServiceRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ServiceRepository {

    override suspend fun getServices(): Resource<List<Service>> {
        return try {
            val result = firestore.collection("services")
                .get()
                .await()

            val services = result.documents.mapNotNull { document ->
                val imageUrl = document.getString("image_url") ?: return@mapNotNull null
                Service(
                    id = document.id,
                    name = document.id,
                    imageUrl = imageUrl
                )
            }

            Resource.Success(services)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}
