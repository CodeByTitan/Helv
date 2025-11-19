package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import ca.unb.mobiledev.handyhub.home.domain.repository.WorkersRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val workersRepository: WorkersRepository
) : ViewModel() {

    private val _providers = MutableStateFlow<Resource<List<Provider>>>(Resource.Loading())
    val providers: StateFlow<Resource<List<Provider>>> = _providers.asStateFlow()

    fun loadProviders(category: String) {
        viewModelScope.launch {
            _providers.value = Resource.Loading()
            _providers.value = workersRepository.getWorkersByCategory(category)
        }
    }
}

