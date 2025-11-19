package ca.unb.mobiledev.handyhub.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.databinding.FragmentServicePickerBinding
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.ServicePickerViewModel
import ca.unb.mobiledev.handyhub.home.presentation.adapter.ServiceCategoryAdapter
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ServicePickerFragment : Fragment() {

    private var _binding: FragmentServicePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServicePickerViewModel by viewModels()
    private lateinit var categoryAdapter: ServiceCategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServicePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadCategories()
        observeCategories()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    private fun setupRecyclerView() {
        categoryAdapter = ServiceCategoryAdapter { categoryName, subcategory ->
            val displayName = subcategory.replace("_", " ")
            val bundle = Bundle().apply {
                putString("categoryName", displayName)
                putString("category", categoryName)
                putString("subcategory", subcategory)
            }
            findNavController().navigate(
                ca.unb.mobiledev.handyhub.R.id.action_servicePickerFragment_to_providersFragment,
                bundle
            )
        }

        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadCategories() {
        val serviceId = arguments?.getString("serviceId") ?: return
        viewModel.loadServiceCategories(serviceId)
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val categories = resource.data ?: emptyList()
                        if (categories.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            categoryAdapter.submitList(categories)
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            resource.message ?: "Failed to load categories",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewCategories.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewCategories.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(ca.unb.mobiledev.handyhub.R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(ca.unb.mobiledev.handyhub.R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
}

