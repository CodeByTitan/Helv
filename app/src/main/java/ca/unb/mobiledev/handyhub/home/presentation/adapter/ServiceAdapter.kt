package ca.unb.mobiledev.handyhub.home.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ItemServiceBinding
import ca.unb.mobiledev.handyhub.home.domain.model.Service
import com.bumptech.glide.Glide

class ServiceAdapter(
    private val onItemClick: (Service) -> Unit
) : ListAdapter<Service, ServiceAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ServiceViewHolder(
        private val binding: ItemServiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(service: Service) {
            binding.apply {
                textViewServiceName.text = service.name
                
                // Load image using Glide
                Glide.with(root.context)
                    .load(service.imageUrl)
                    .placeholder(R.drawable.ic_services) // Fallback image
                    .error(R.drawable.ic_services) // Error image
                    .into(imageViewService)
                
                root.setOnClickListener {
                    onItemClick(service)
                }
            }
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Service>() {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem == newItem
        }
    }
}
