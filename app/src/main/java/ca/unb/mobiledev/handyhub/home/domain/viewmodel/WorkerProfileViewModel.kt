package ca.unb.mobiledev.handyhub.home.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.handyhub.home.domain.model.Booking
import ca.unb.mobiledev.handyhub.home.domain.model.WorkerDetail
import ca.unb.mobiledev.handyhub.home.domain.repository.WorkersRepository
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkerProfileViewModel @Inject constructor(
    private val repository: WorkersRepository
) : ViewModel() {
    
    private val _workerDetail = MutableStateFlow<Resource<WorkerDetail>>(Resource.Loading())
    val workerDetail: StateFlow<Resource<WorkerDetail>> = _workerDetail.asStateFlow()
    
    private val _bookings = MutableStateFlow<Resource<List<Booking>>>(Resource.Success(emptyList()))
    val bookings: StateFlow<Resource<List<Booking>>> = _bookings.asStateFlow()
    
    private val _bookingResult = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val bookingResult: StateFlow<Resource<String>> = _bookingResult.asStateFlow()
    
    fun loadWorkerDetail(workerId: String) {
        viewModelScope.launch {
            _workerDetail.value = Resource.Loading()
            _workerDetail.value = repository.getWorkerDetail(workerId)
        }
    }
    
    fun loadBookingsForDate(workerId: String, date: String) {
        viewModelScope.launch {
            _bookings.value = Resource.Loading()
            _bookings.value = repository.getBookingsForWorkerOnDate(workerId, date)
        }
    }
    
    fun createBooking(
        workerId: String,
        userId: String,
        userName: String,
        serviceCategory: String,
        serviceName: String,
        date: String,
        startTime: String,
        endTime: String,
        totalAmount: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            _bookingResult.value = Resource.Loading()
            _bookingResult.value = repository.createBooking(
                workerId, userId, userName, serviceCategory, serviceName,
                date, startTime, endTime, totalAmount, notes
            )
        }
    }
}

