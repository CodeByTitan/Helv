package ca.unb.mobiledev.handyhub.home.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.databinding.ItemProviderBinding
import ca.unb.mobiledev.handyhub.home.domain.model.Provider
import com.bumptech.glide.Glide

class ProviderAdapter(
    private val onItemClick: (Provider) -> Unit,
    private val onGetServiceClick: (Provider) -> Unit
) : ListAdapter<Provider, ProviderAdapter.ProviderViewHolder>(ProviderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val binding = ItemProviderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProviderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ProviderViewHolder(
        private val binding: ItemProviderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(provider: Provider, position: Int) {
            binding.apply {
                textViewProviderName.text = provider.fullName
                textViewCategory.text = provider.category
                textViewLocation.text = provider.location
                textViewDistance.text = "• ${provider.distance}"
                textViewRating.text = String.format("%.1f", provider.rating)
                
                val priceText = when {
                    provider.pricePerHour != null -> "$${provider.pricePerHour}/hr"
                    provider.pricePerSession != null -> "$${provider.pricePerSession}/session"
                    else -> "Contact for pricing"
                }
                textViewPrice.text = priceText
                
                textViewAvailability.text = if (provider.isAvailable) "Available" else "Busy"
                val availabilityColor = if (provider.isAvailable) 
                    0xFF4CAF50.toInt() 
                else 
                    0xFF999999.toInt()
                textViewAvailability.setTextColor(availabilityColor)
                
                val drawables = textViewAvailability.compoundDrawables
                val startDrawable = drawables[0]
                if (startDrawable != null) {
                    val wrappedDrawable = DrawableCompat.wrap(startDrawable.mutate())
                    DrawableCompat.setTint(wrappedDrawable, availabilityColor)
                    textViewAvailability.setCompoundDrawablesWithIntrinsicBounds(
                        wrappedDrawable, null, null, null
                    )
                }
                
                Glide.with(root.context)
                    .load(provider.imageUrl)
                    .placeholder(ca.unb.mobiledev.handyhub.R.drawable.worker)
                    .error(ca.unb.mobiledev.handyhub.R.drawable.worker)
                    .circleCrop()
                    .into(imageViewProvider)
                
                root.setOnClickListener {
                    onItemClick(provider)
                }
                
                buttonGetService.setOnClickListener {
                    onGetServiceClick(provider)
                }
            }
        }
    }

    class ProviderDiffCallback : DiffUtil.ItemCallback<Provider>() {
        override fun areItemsTheSame(oldItem: Provider, newItem: Provider): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Provider, newItem: Provider): Boolean {
            return oldItem == newItem
        }
    }
}


