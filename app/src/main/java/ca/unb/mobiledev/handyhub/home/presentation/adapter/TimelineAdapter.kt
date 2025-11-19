package ca.unb.mobiledev.handyhub.home.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ItemTimelineMilestoneBinding
import ca.unb.mobiledev.handyhub.home.domain.model.JobMilestone

class TimelineAdapter : ListAdapter<JobMilestone, TimelineAdapter.TimelineViewHolder>(TimelineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineMilestoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    inner class TimelineViewHolder(
        private val binding: ItemTimelineMilestoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(milestone: JobMilestone, isLast: Boolean) {
            binding.apply {
                textViewMilestoneTitle.text = milestone.title
                textViewMilestoneTime.text = milestone.time

                val context = root.context
                if (milestone.isCompleted) {
                    imageViewMilestone.setImageResource(R.drawable.ic_check_circle)
                    imageViewMilestone.setColorFilter(0xFF4CAF50.toInt())
                    textViewMilestoneTitle.setTextColor(0xFF000000.toInt())
                } else if (milestone.isCurrent) {
                    imageViewMilestone.setImageResource(R.drawable.ic_radio_button_checked)
                    imageViewMilestone.setColorFilter(0xFFFF5722.toInt())
                    textViewMilestoneTitle.setTextColor(0xFFFF5722.toInt())
                } else {
                    imageViewMilestone.setImageResource(R.drawable.ic_radio_button_unchecked)
                    imageViewMilestone.setColorFilter(0xFF999999.toInt())
                    textViewMilestoneTitle.setTextColor(0xFF999999.toInt())
                }

                viewTimelineLine.visibility = if (isLast) ViewGroup.GONE else ViewGroup.VISIBLE
            }
        }
    }

    class TimelineDiffCallback : DiffUtil.ItemCallback<JobMilestone>() {
        override fun areItemsTheSame(oldItem: JobMilestone, newItem: JobMilestone): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: JobMilestone, newItem: JobMilestone): Boolean {
            return oldItem == newItem
        }
    }
}

