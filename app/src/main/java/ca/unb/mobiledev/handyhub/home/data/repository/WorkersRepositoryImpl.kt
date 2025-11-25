package ca.unb.mobiledev.handyhub.home.data.repository

import android.util.Log
import ca.unb.mobiledev.handyhub.home.domain.model.DaySchedule
import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import ca.unb.mobiledev.handyhub.home.domain.model.TimeSlot
import ca.unb.mobiledev.handyhub.home.domain.model.WorkerDetail
import ca.unb.mobiledev.handyhub.home.domain.repository.WorkersRepository
import ca.unb.mobiledev.handyhub.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WorkersRepository {

    override suspend fun getWorkersByCategory(category: String): Resource<List<Provider>> {
        return try {
            Log.d("WorkersRepository", "Querying for category: '$category'")
            
            val result = firestore.collection("workers")
                .whereEqualTo("category", category)
                .whereEqualTo("is_active", true)
                .get()
                .await()
            
            Log.d("WorkersRepository", "Found ${result.documents.size} workers with category '$category'")
            val providers = parseProviders(result.documents)
            
            Resource.Success(providers)
        } catch (e: Exception) {
            Log.e("WorkersRepository", "Error loading workers: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to load workers: ${e.localizedMessage}")
        }
    }
    
    private fun parseProviders(documents: List<com.google.firebase.firestore.DocumentSnapshot>): List<Provider> {
        return documents.mapNotNull { document ->
            try {
                val name = document.getString("name") ?: return@mapNotNull null
                val workerCategory = document.getString("category") ?: ""
                Log.d("WorkersRepository", "Worker '$name' has category: '$workerCategory'")
                val city = document.getString("city") ?: ""
                val state = document.getString("state") ?: ""
                val location = if (city.isNotEmpty() && state.isNotEmpty()) {
                    "$city, $state"
                } else if (city.isNotEmpty()) {
                    city
                } else {
                    "Location not specified"
                }
                
                val rating = document.getDouble("rating")?.toDouble() ?: 0.0
                val hourlyRate = document.getLong("hourly_rate")?.toInt()
                val profilePicture = document.getString("profile_picture") ?: ""
                val availabilityStatus = document.getString("availability_status") ?: "unavailable"
                val isAvailable = availabilityStatus.lowercase() == "available"
                
                Provider(
                    id = document.id,
                    fullName = name,
                    category = workerCategory,
                    location = location,
                    distance = "N/A",
                    rating = rating,
                    pricePerHour = hourlyRate,
                    imageUrl = profilePicture,
                    isAvailable = isAvailable
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    override suspend fun getWorkerDetail(workerId: String): Resource<WorkerDetail> {
        return try {
            Log.d("WorkersRepository", "Fetching worker detail for ID: '$workerId'")
            
            val document = firestore.collection("workers")
                .document(workerId)
                .get()
                .await()
            
            if (!document.exists()) {
                return Resource.Error("Worker not found")
            }
            
            val workerDetail = parseWorkerDetail(document)
            Resource.Success(workerDetail)
        } catch (e: Exception) {
            Log.e("WorkersRepository", "Error loading worker detail: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to load worker detail: ${e.localizedMessage}")
        }
    }
    
    private fun parseWorkerDetail(document: com.google.firebase.firestore.DocumentSnapshot): WorkerDetail {
        val name = document.getString("name") ?: ""
        val email = document.getString("email") ?: ""
        val phone = document.getString("phone") ?: ""
        val category = document.getString("category") ?: ""
        val subcategory = document.getString("subcategory") ?: ""
        val topLevelCategory = document.getString("topLevelCategory") ?: ""
        val rating = document.getDouble("rating")?.toDouble() ?: 0.0
        val hourlyRate = document.getLong("hourly_rate")?.toInt() ?: 0
        val profilePicture = document.getString("profile_picture") ?: ""
        val isVerified = document.getBoolean("is_verified") ?: false
        val isActive = document.getBoolean("is_active") ?: true
        val city = document.getString("city") ?: ""
        val state = document.getString("state") ?: ""
        val scheduleTimezone = document.getString("schedule_timezone") ?: "America/New_York"
        val slotDurationMinutes = document.getLong("slot_duration_minutes")?.toInt() ?: 60
        val bufferTimeMinutes = document.getLong("buffer_time_minutes")?.toInt() ?: 15
        val advanceBookingDays = document.getLong("advance_booking_days")?.toInt() ?: 30
        val minNoticeHours = document.getLong("min_notice_hours")?.toInt() ?: 2
        val acceptsSameDayBooking = document.getBoolean("accepts_same_day_booking") ?: false
        val maxBookingsPerDay = document.getLong("max_bookings_per_day")?.toInt() ?: 8
        
        // Parse date-based schedule (new format: schedule[date] = {day, is_available, slots})
        val scheduleMap = mutableMapOf<String, DaySchedule>()
        val scheduleData = document.get("schedule") as? Map<*, *>
        
        if (scheduleData != null) {
            Log.d("WorkersRepository", "Parsing date-based schedule with ${scheduleData.size} dates")
            for ((dateKey, dateData) in scheduleData) {
                if (dateData is Map<*, *>) {
                    val day = dateData["day"] as? String ?: ""
                    val isAvailable = (dateData["is_available"] as? Boolean) 
                        ?: (dateData["isAvailable"] as? Boolean) 
                        ?: false
                    val slotsData = dateData["slots"] as? List<*>
                    val slots = mutableListOf<TimeSlot>()
                    
                    Log.d("WorkersRepository", "Date: $dateKey, day: $day, isAvailable: $isAvailable, slots count: ${slotsData?.size ?: 0}")
                    
                    if (slotsData != null) {
                        for (slotData in slotsData) {
                            if (slotData is Map<*, *>) {
                                val start = slotData["start"] as? String ?: ""
                                val end = slotData["end"] as? String ?: ""
                                if (start.isNotEmpty() && end.isNotEmpty()) {
                                    slots.add(TimeSlot(start = start, end = end))
                                    Log.d("WorkersRepository", "Added slot: $start - $end")
                                }
                            }
                        }
                    }
                    
                    scheduleMap[dateKey.toString()] = DaySchedule(
                        day = day,
                        isAvailable = isAvailable,
                        slots = slots
                    )
                }
            }
            Log.d("WorkersRepository", "Parsed ${scheduleMap.size} dates in schedule")
        } else {
            Log.d("WorkersRepository", "No schedule data found")
        }
        
        return WorkerDetail(
            id = document.id,
            name = name,
            email = email,
            phone = phone,
            category = category,
            subcategory = subcategory,
            topLevelCategory = topLevelCategory,
            rating = rating,
            hourlyRate = hourlyRate,
            profilePicture = profilePicture,
            isVerified = isVerified,
            isActive = isActive,
            city = city,
            state = state,
            schedule = scheduleMap,
            scheduleTimezone = scheduleTimezone,
            slotDurationMinutes = slotDurationMinutes,
            bufferTimeMinutes = bufferTimeMinutes,
            advanceBookingDays = advanceBookingDays,
            minNoticeHours = minNoticeHours,
            acceptsSameDayBooking = acceptsSameDayBooking,
            maxBookingsPerDay = maxBookingsPerDay
        )
    }
    
    override suspend fun getBookingsForWorkerOnDate(workerId: String, date: String): Resource<List<ca.unb.mobiledev.handyhub.home.domain.model.Booking>> {
        return try {
            val querySnapshot = firestore.collection("workers")
                .document(workerId)
                .collection("bookings")
                .whereEqualTo("date", date)
                .whereIn("status", listOf("confirmed", "pending"))
                .get()
                .await()
            
            val bookings = querySnapshot.documents.mapNotNull { document ->
                try {
                    ca.unb.mobiledev.handyhub.home.domain.model.Booking(
                        id = document.id,
                        userId = document.getString("user_id") ?: "",
                        userName = document.getString("user_name") ?: "",
                        workerId = workerId,
                        serviceCategory = document.getString("service_category") ?: "",
                        serviceName = document.getString("service_name") ?: "",
                        date = document.getString("date") ?: "",
                        startTime = document.getString("start_time") ?: "",
                        endTime = document.getString("end_time") ?: "",
                        status = document.getString("status") ?: "pending",
                        totalAmount = document.getLong("total_amount")?.toInt() ?: 0,
                        notes = document.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("WorkersRepository", "Error parsing booking: ${e.message}", e)
                    null
                }
            }
            
            Resource.Success(bookings)
        } catch (e: Exception) {
            Log.e("WorkersRepository", "Error getting bookings: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to get bookings")
        }
    }
    
    override suspend fun createBooking(
        workerId: String,
        userId: String,
        userName: String,
        serviceCategory: String,
        serviceName: String,
        date: String,
        startTime: String,
        endTime: String,
        totalAmount: Int,
        notes: String
    ): Resource<String> {
        return try {
            val bookingData = hashMapOf(
                "user_id" to userId,
                "user_name" to userName,
                "service_category" to serviceCategory,
                "service_name" to serviceName,
                "date" to date,
                "start_time" to startTime,
                "end_time" to endTime,
                "status" to "pending",
                "total_amount" to totalAmount,
                "notes" to notes,
                "created_at" to com.google.firebase.Timestamp.now(),
                "updated_at" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = firestore.collection("workers")
                .document(workerId)
                .collection("bookings")
                .add(bookingData)
                .await()
            
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Log.e("WorkersRepository", "Error creating booking: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to create booking")
        }
    }
}
