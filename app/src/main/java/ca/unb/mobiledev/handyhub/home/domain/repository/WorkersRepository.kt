package ca.unb.mobiledev.handyhub.home.domain.repository

import ca.unb.mobiledev.handyhub.home.domain.model.Booking
import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import ca.unb.mobiledev.handyhub.home.domain.model.WorkerDetail
import ca.unb.mobiledev.handyhub.util.Resource

interface WorkersRepository {
    suspend fun getWorkersByCategory(category: String): Resource<List<Provider>>
    suspend fun getWorkerDetail(workerId: String): Resource<WorkerDetail>
    suspend fun getBookingsForWorkerOnDate(workerId: String, date: String): Resource<List<Booking>>
    suspend fun createBooking(
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
    ): Resource<String>
}

