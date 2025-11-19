package ca.unb.mobiledev.handyhub.home.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.databinding.ItemServiceCategoryBinding
import ca.unb.mobiledev.handyhub.home.domain.model.ServiceCategory

class ServiceCategoryAdapter(
    private val onSubcategoryClick: (String, String) -> Unit
) : ListAdapter<ServiceCategory, ServiceCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemServiceCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemServiceCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ServiceCategory) {
            binding.textViewCategoryName.text = category.categoryName.replace("_", " ")
            
            binding.layoutSubcategories.removeAllViews()
            
            category.subcategories.forEach { subcategory ->
                val subcategoryView = LayoutInflater.from(binding.root.context)
                    .inflate(R.layout.item_subcategory, binding.layoutSubcategories, false)
                
                val textView = subcategoryView.findViewById<TextView>(R.id.textViewSubcategory)
                val displayName = subcategory.replace("_", " ")
                textView.text = displayName
                
                subcategoryView.setOnClickListener {
                    onSubcategoryClick(category.categoryName, subcategory)
                }
                
                binding.layoutSubcategories.addView(subcategoryView)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<ServiceCategory>() {
        override fun areItemsTheSame(oldItem: ServiceCategory, newItem: ServiceCategory): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }

        override fun areContentsTheSame(oldItem: ServiceCategory, newItem: ServiceCategory): Boolean {
            return oldItem == newItem
        }
    }
}


