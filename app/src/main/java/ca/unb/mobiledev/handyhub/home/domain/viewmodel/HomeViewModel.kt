package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _services = MutableStateFlow<Resource<List<Service>>>(Resource.Loading())
    val services: StateFlow<Resource<List<Service>>> = _services.asStateFlow()

    init {
        loadServices()
    }

    private fun loadServices() {
        viewModelScope.launch {
            _services.value = Resource.Loading()
            _services.value = serviceRepository.getServices()
        }
    }

    fun refreshServices() {
        loadServices()
    }
}
