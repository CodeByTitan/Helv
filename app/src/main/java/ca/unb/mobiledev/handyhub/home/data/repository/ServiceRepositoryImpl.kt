package ca.unb.mobiledev.handyhub.home.data.repository

import ca.unb.mobiledev.handyhub.home.domain.model.SearchResult
import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.home.domain.model.ServiceCategory
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

    override suspend fun getServiceCategories(serviceId: String): Resource<List<ServiceCategory>> {
        return try {
            val serviceDocRef = firestore.collection("services").document(serviceId)
            val serviceData = serviceDocRef.get().await()
            
            if (!serviceData.exists()) {
                return Resource.Error("Service document not found: $serviceId")
            }
            
            val categories = mutableListOf<ServiceCategory>()
            
            val collectionIds = when {
                serviceData.get("subcollections") != null -> {
                    @Suppress("UNCHECKED_CAST")
                    (serviceData.get("subcollections") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                }
                serviceData.get("collections") != null -> {
                    @Suppress("UNCHECKED_CAST")
                    (serviceData.get("collections") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                }
                serviceData.get("categories") != null -> {
                    @Suppress("UNCHECKED_CAST")
                    (serviceData.get("categories") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                }
                else -> emptyList()
            }
            
            if (collectionIds.isEmpty()) {
                return Resource.Error("No subcollections found. Please add a 'subcollections' field to the service document with an array of subcollection names (e.g., ['Quick_Assist', 'Other_Collection']).")
            }
            
            for (midLevelCategory in collectionIds) {
                try {
                    val subcollectionRef = serviceDocRef.collection(midLevelCategory)
                    val subcollectionDocs = subcollectionRef.get().await()
                    
                    val validSubcategories = subcollectionDocs.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: doc.id
                        
                        if (isValidServiceName(name)) {
                            name
                        } else {
                            null
                        }
                    }
                    
                    if (validSubcategories.isNotEmpty()) {
                        categories.add(
                            ServiceCategory(
                                categoryName = midLevelCategory,
                                subcategories = validSubcategories
                            )
                        )
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            if (categories.isEmpty()) {
                Resource.Error("No valid subcategories found in any subcollection")
            } else {
                Resource.Success(categories)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load service categories: ${e.localizedMessage}")
        }
    }
    
    private fun isValidServiceName(name: String): Boolean {
        return when {
            name.contains(" ") -> false
            name.contains("_") -> true
            else -> !name.contains(" ")
        }
    }

    override suspend fun searchServices(query: String): Resource<List<SearchResult>> {
        return try {
            if (query.length < 2) {
                return Resource.Success(emptyList())
            }

            val searchQuery = query.lowercase().trim()
            val results = mutableListOf<SearchResult>()

            val servicesSnapshot = firestore.collection("services").get().await()

            for (serviceDoc in servicesSnapshot.documents) {
                val serviceId = serviceDoc.id
                
                val collectionIds = when {
                    serviceDoc.get("subcollections") != null -> {
                        @Suppress("UNCHECKED_CAST")
                        (serviceDoc.get("subcollections") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                    }
                    serviceDoc.get("collections") != null -> {
                        @Suppress("UNCHECKED_CAST")
                        (serviceDoc.get("collections") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                    }
                    serviceDoc.get("categories") != null -> {
                        @Suppress("UNCHECKED_CAST")
                        (serviceDoc.get("categories") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                    }
                    else -> emptyList()
                }

                if (collectionIds.isEmpty()) continue

                for (midLevelCategory in collectionIds) {
                    try {
                        val subcollectionRef = serviceDoc.reference.collection(midLevelCategory)
                        val subcollectionDocs = subcollectionRef.get().await()

                        for (subcategoryDoc in subcollectionDocs.documents) {
                            val name = subcategoryDoc.getString("name") ?: subcategoryDoc.id
                            
                            if (!isValidServiceName(name)) continue
                            
                            if (name.lowercase().contains(searchQuery)) {
                                results.add(
                                    SearchResult(
                                        subcategoryName = name,
                                        midLevelCategory = midLevelCategory,
                                        serviceId = serviceId
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to search services")
        }
    }
}
