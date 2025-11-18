package ca.unb.mobiledev.handyhub.util

import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

fun Timestamp.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val messageTime = this.toDate().time
    val diff = now - messageTime

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes min ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days day${if (days > 1) "s" else ""} ago"
        }
        else -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            "$weeks week${if (weeks > 1) "s" else ""} ago"
        }
    }
}

fun Timestamp.toTimeString(): String {
    val date = this.toDate()
    val hour = date.hours
    val minute = date.minutes
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}

