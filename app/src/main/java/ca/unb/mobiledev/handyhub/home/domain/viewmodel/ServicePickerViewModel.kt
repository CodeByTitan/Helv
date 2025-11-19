package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.home.domain.model.ServiceCategory
import ca.unb.mobiledev.handyhub.home.domain.repository.ServiceRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServicePickerViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<Resource<List<ServiceCategory>>>(Resource.Loading())
    val categories: StateFlow<Resource<List<ServiceCategory>>> = _categories.asStateFlow()

    fun loadServiceCategories(serviceId: String) {
        viewModelScope.launch {
            _categories.value = Resource.Loading()
            _categories.value = serviceRepository.getServiceCategories(serviceId)
        }
    }
}



