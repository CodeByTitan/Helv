package ca.unb.mobiledev.handyhub.home.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.databinding.ItemTimeSlotBinding
import ca.unb.mobiledev.handyhub.home.domain.model.TimeSlot

data class AvailableTimeSlot(
    val timeSlot: TimeSlot,
    val isBooked: Boolean = false
)

class TimeSlotAdapter(
    private val onSlotClick: (TimeSlot) -> Unit
) : ListAdapter<AvailableTimeSlot, TimeSlotAdapter.TimeSlotViewHolder>(TimeSlotDiffCallback()) {

    private var selectedSlot: TimeSlot? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeSlotViewHolder(
        private val binding: ItemTimeSlotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(availableSlot: AvailableTimeSlot) {
            val timeSlot = availableSlot.timeSlot
            val isSelected = timeSlot == selectedSlot
            val isBooked = availableSlot.isBooked

            binding.textViewTimeSlot.text = "${timeSlot.start} - ${timeSlot.end}"

            val backgroundColor = when {
                isBooked -> 0xFFE0E0E0.toInt()
                isSelected -> 0xFFFF5722.toInt()
                else -> 0xFFFFFFFF.toInt()
            }

            val textColor = when {
                isBooked -> 0xFF999999.toInt()
                isSelected -> 0xFFFFFFFF.toInt()
                else -> 0xFF000000.toInt()
            }

            binding.root.setCardBackgroundColor(backgroundColor)
            binding.textViewTimeSlot.setTextColor(textColor)
            binding.root.isEnabled = !isBooked

            binding.root.setOnClickListener {
                if (!isBooked) {
                    selectedSlot = timeSlot
                    notifyDataSetChanged()
                    onSlotClick(timeSlot)
                }
            }
        }
    }

    class TimeSlotDiffCallback : DiffUtil.ItemCallback<AvailableTimeSlot>() {
        override fun areItemsTheSame(oldItem: AvailableTimeSlot, newItem: AvailableTimeSlot): Boolean {
            return oldItem.timeSlot.start == newItem.timeSlot.start && 
                   oldItem.timeSlot.end == newItem.timeSlot.end
        }

        override fun areContentsTheSame(oldItem: AvailableTimeSlot, newItem: AvailableTimeSlot): Boolean {
            return oldItem == newItem
        }
    }
}

