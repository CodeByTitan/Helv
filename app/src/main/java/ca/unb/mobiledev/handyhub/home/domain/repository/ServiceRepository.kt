package ca.unb.mobiledev.handyhub.home.domain.repository

import ca.unb.mobiledev.handyhub.home.domain.model.SearchResult
import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.home.domain.model.ServiceCategory
import ca.unb.mobiledev.handyhub.util.Resource

interface ServiceRepository {
    suspend fun getServices(): Resource<List<Service>>
    suspend fun getServiceCategories(serviceId: String): Resource<List<ServiceCategory>>
    suspend fun searchServices(query: String): Resource<List<SearchResult>>
}
