package ca.unb.mobiledev.handyhub.home.presentation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ca.unb.mobiledev.handyhub.databinding.FragmentProvidersBinding
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.ProvidersViewModel
import ca.unb.mobiledev.handyhub.home.presentation.adapter.ProviderAdapter
import ca.unb.mobiledev.handyhub.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProvidersFragment : Fragment() {

    private var _binding: FragmentProvidersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProvidersViewModel by viewModels()
    private lateinit var providerAdapter: ProviderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProvidersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadProviders()
        observeProviders()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    private fun setupRecyclerView() {
        providerAdapter = ProviderAdapter(
            onItemClick = { provider ->
                navigateToWorkerProfile(provider.id, scrollToAvailability = false)
            },
            onGetServiceClick = { provider ->
                navigateToWorkerProfile(provider.id, scrollToAvailability = true)
            }
        )

        binding.recyclerViewProviders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = providerAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadProviders() {
        val categoryName = arguments?.getString("categoryName") ?: "Service"
        val subcategory = arguments?.getString("subcategory") ?: categoryName
        val categoryWithoutUnderscores = subcategory.replace("_", " ")
        binding.textViewTitle.text = "$categoryName providers near you"
        binding.textViewLoading.text = "Searching for \"$categoryName\" service providers near you"
        viewModel.loadProviders(categoryWithoutUnderscores)
    }

    private fun observeProviders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.providers.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val providers = resource.data ?: emptyList()
                        if (providers.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            providerAdapter.submitList(providers) {
                                animateCardsIn()
                            }
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        showEmptyState(true)
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Error: ${resource.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewProviders.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.textViewEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewProviders.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun animateCardsIn() {
        binding.recyclerViewProviders.post {
            val layoutManager = binding.recyclerViewProviders.layoutManager as LinearLayoutManager
            val itemCount = layoutManager.itemCount

            for (i in 0 until itemCount) {
                val view = layoutManager.findViewByPosition(i) ?: continue
                view.alpha = 0f
                view.translationY = 300f

                val animator = ObjectAnimator.ofFloat(view, "translationY", 300f, 0f)
                val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(animator, alphaAnimator)
                animatorSet.duration = 500
                animatorSet.interpolator = DecelerateInterpolator()
                animatorSet.startDelay = (i * 100).toLong()
                animatorSet.start()
            }
        }
    }

    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(ca.unb.mobiledev.handyhub.R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(ca.unb.mobiledev.handyhub.R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.VISIBLE
    }

    private fun navigateToWorkerProfile(workerId: String, scrollToAvailability: Boolean) {
        val categoryName = arguments?.getString("categoryName") ?: "Service"
        val action = ProvidersFragmentDirections.actionProvidersFragmentToWorkerProfileFragment(
            workerId = workerId,
            scrollToAvailability = scrollToAvailability,
            categoryName = categoryName
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
}

