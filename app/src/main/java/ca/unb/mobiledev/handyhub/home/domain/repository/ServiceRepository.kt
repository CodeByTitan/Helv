package ca.unb.mobiledev.handyhub.home.domain.repository

import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.util.Resource

interface ServiceRepository {
    suspend fun getServices(): Resource<List<Service>>
}
