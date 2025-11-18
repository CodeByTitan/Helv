package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.auth.domain.repository.AuthRepository
import ca.unb.mobiledev.handyhub.home.domain.model.Service
import ca.unb.mobiledev.handyhub.home.domain.repository.ServiceRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _services = MutableStateFlow<Resource<List<Service>>>(Resource.Loading())
    val services: StateFlow<Resource<List<Service>>> = _services.asStateFlow()
    
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    init {
        loadServices()
        loadUserName()
    }

    private fun loadServices() {
        viewModelScope.launch {
            _services.value = Resource.Loading()
            _services.value = serviceRepository.getServices()
        }
    }
    
    private fun loadUserName() {
        viewModelScope.launch {
            val cachedName = authRepository.getCachedUserName()
            
            if (cachedName != null) {
                _userName.value = cachedName.split(" ").firstOrNull() ?: "User"
            } else {
                authRepository.getCurrentUser().collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _userName.value = resource.data?.name?.split(" ")?.firstOrNull() ?: "User"
                        }
                        is Resource.Error -> {
                            _userName.value = "User"
                        }
                        is Resource.Loading -> {}
                    }
                }
            }
        }
    }

    fun refreshServices() {
        loadServices()
    }
}
