package ca.unb.mobiledev.handyhub.home.domain.model

data class WorkerDetail(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val category: String = "",
    val subcategory: String = "",
    val topLevelCategory: String = "",
    val rating: Double = 0.0,
    val hourlyRate: Int = 0,
    val profilePicture: String = "",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val city: String = "",
    val state: String = "",
    val schedule: Map<String, DaySchedule> = emptyMap(), // Changed from weeklySchedule to schedule
    val scheduleTimezone: String = "America/New_York",
    val slotDurationMinutes: Int = 60,
    val bufferTimeMinutes: Int = 15,
    val advanceBookingDays: Int = 30,
    val minNoticeHours: Int = 2,
    val acceptsSameDayBooking: Boolean = false,
    val maxBookingsPerDay: Int = 8
)

data class DaySchedule(
    val day: String = "", // e.g., "monday", "tuesday"
    val isAvailable: Boolean = false,
    val slots: List<TimeSlot> = emptyList()
)

data class TimeSlot(
    val start: String,
    val end: String
)

