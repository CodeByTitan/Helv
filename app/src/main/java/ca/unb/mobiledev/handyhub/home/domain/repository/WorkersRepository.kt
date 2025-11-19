package ca.unb.mobiledev.handyhub.home.domain.repository

import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import ca.unb.mobiledev.handyhub.util.Resource

interface WorkersRepository {
    suspend fun getWorkersByCategory(category: String): Resource<List<Provider>>
}

