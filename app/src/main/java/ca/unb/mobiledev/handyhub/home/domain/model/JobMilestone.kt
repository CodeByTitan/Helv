package ca.unb.mobiledev.handyhub.home.domain.model

data class JobMilestone(
    val title: String,
    val time: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

