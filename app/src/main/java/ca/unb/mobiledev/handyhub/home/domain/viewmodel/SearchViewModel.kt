package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.home.domain.model.SearchResult
import ca.unb.mobiledev.handyhub.home.domain.repository.ServiceRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<SearchResult>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<SearchResult>>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        
        if (query.length < 2) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _searchResults.value = Resource.Loading()
            _searchResults.value = serviceRepository.searchServices(query)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = Resource.Success(emptyList())
    }
}



