package ca.unb.mobiledev.handyhub.messages.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ItemTopHelperBinding
import ca.unb.mobiledev.handyhub.messages.domain.model.TopHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop

class TopHelperAdapter(
    private val onItemClick: (TopHelper) -> Unit
) : ListAdapter<TopHelper, TopHelperAdapter.TopHelperViewHolder>(TopHelperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopHelperViewHolder {
        val binding = ItemTopHelperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopHelperViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopHelperViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopHelperViewHolder(
        private val binding: ItemTopHelperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(helper: TopHelper) {
            binding.apply {
                textViewHelperName.text = helper.fullName
                textViewCategory.text = helper.category
                textViewLocation.text = helper.location
                textViewDistance.text = "• ${helper.distance}"
                textViewRating.text = helper.rating.toString()
                
                val price = if (helper.pricePerHour != null) {
                    "$${helper.pricePerHour}/hr"
                } else {
                    "$${helper.pricePerSession}/session"
                }
                textViewPrice.text = price
                
                if (helper.isAvailable) {
                    textViewAvailability.text = "Available"
                    textViewAvailability.setTextColor(root.context.getColor(R.color.green_500))
                    textViewAvailability.setBackgroundResource(R.drawable.availability_background)
                    textViewAvailability.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, 0, 0)
                } else {
                    textViewAvailability.text = "Busy"
                    textViewAvailability.setTextColor(root.context.getColor(R.color.gray_600))
                    textViewAvailability.setBackgroundResource(R.drawable.busy_background)
                    textViewAvailability.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                
                Glide.with(root.context)
                    .load(helper.imageUrl)
                    .transform(CircleCrop())
                    .into(imageViewHelper)
                
                root.setOnClickListener {
                    onItemClick(helper)
                }
            }
        }
    }

    class TopHelperDiffCallback : DiffUtil.ItemCallback<TopHelper>() {
        override fun areItemsTheSame(oldItem: TopHelper, newItem: TopHelper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TopHelper, newItem: TopHelper): Boolean {
            return oldItem == newItem
        }
    }
}

