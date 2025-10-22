package ca.unb.mobiledev.handyhub.auth.presentation.onboarding_steps

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ca.unb.mobiledev.handyhub.auth.presentation.OnboardingActivity
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel
import ca.unb.mobiledev.handyhub.auth.presentation.widgets.AuthCardManager
import ca.unb.mobiledev.handyhub.auth.presentation.widgets.AuthFlowObserver
import ca.unb.mobiledev.handyhub.databinding.FragmentSignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {
    
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var getStartedManager: AuthCardManager
    private lateinit var joinUsManager: AuthCardManager
    private lateinit var flowObserver: AuthFlowObserver
    
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeManagers()
        setupUI()
        observeFlows()
    }
    
    private fun initializeManagers() {
        getStartedManager = AuthCardManager(this, authViewModel, true)
        joinUsManager = AuthCardManager(this, authViewModel, false)
        flowObserver = AuthFlowObserver(this, authViewModel)
    }
    
    private fun setupUI() {
        loadLogo()
        setupSlogan()
        setupCards()
        setupSkipButton()
    }
    
    private fun loadLogo() {
        try {
            requireContext().assets.open("Helv_New_Logo.png").use { inputStream ->
                binding.helvLogo.setImageBitmap(BitmapFactory.decodeStream(inputStream))
            }
        } catch (e: Exception) {
        }
    }
    
    private fun setupSlogan() {
        val fullText = "Helping you with very much"
        val spannable = SpannableString(fullText)
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.orange_500)
        val veryStart = fullText.indexOf("very")
        
        spannable.setSpan(
            ForegroundColorSpan(orangeColor),
            veryStart,
            veryStart + "very".length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.sloganText.text = spannable
    }
    
    private fun setupCards() {
        val cardWidth = (resources.displayMetrics.widthPixels * 0.6).toInt()
        
        setupCard(
            card = binding.getStartedCard,
            manager = getStartedManager,
            buttonTitle = "Get Started",
            expandedTitle = "Start getting help",
            subtitle = "Connect with local service providers",
            cardWidth = cardWidth,
            otherCard = binding.joinUsCard
        )
        
        setupCard(
            card = binding.joinUsCard,
            manager = joinUsManager,
            buttonTitle = "Become a Helper",
            expandedTitle = "Join the Community of Helpers",
            subtitle = "Connect with local service providers",
            cardWidth = cardWidth,
            otherCard = binding.getStartedCard
        )
    }
    
    private fun setupCard(
        card: ca.unb.mobiledev.handyhub.auth.presentation.widgets.AuthCardView,
        manager: AuthCardManager,
        buttonTitle: String,
        expandedTitle: String,
        subtitle: String,
        cardWidth: Int,
        otherCard: ca.unb.mobiledev.handyhub.auth.presentation.widgets.AuthCardView
    ) {
        card.layoutParams = (card.layoutParams as ViewGroup.MarginLayoutParams).apply {
            width = cardWidth
        }
        
        card.apply {
            setButtonTitle(buttonTitle)
            setExpandedTitle(expandedTitle)
            setSubtitle(subtitle)
            
            manager.inflateContent(getContentContainer())
            
            onExpandClick = { expand(otherCard, true) }
            onCloseClick = {
                collapse(otherCard)
                manager.getHandler().reset()
            }
        }
    }
    
    private fun observeFlows() {
        flowObserver.observeFlows(
            getStartedManager,
            joinUsManager,
            binding.getStartedCard,
            binding.joinUsCard
        )
        
        flowObserver.handleEmailCheckResult(
            getStartedManager.getContent(),
            joinUsManager.getContent()
        )
        
        flowObserver.handlePhoneCheckResult(
            getStartedManager.getContent(),
            joinUsManager.getContent()
        )
    }
    
    private fun setupSkipButton() {
        binding.buttonSkip.setOnClickListener {
            // Skip functionality removed - user must complete authentication
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
