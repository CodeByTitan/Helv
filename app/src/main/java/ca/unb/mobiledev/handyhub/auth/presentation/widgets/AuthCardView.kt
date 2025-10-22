package ca.unb.mobiledev.handyhub.auth.presentation.widgets

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import ca.unb.mobiledev.handyhub.databinding.ViewAuthCardBinding

class AuthCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewAuthCardBinding = ViewAuthCardBinding.inflate(
        LayoutInflater.from(context), this
    )
    
    var isExpanded = false
        private set
    
    var onCloseClick: (() -> Unit)? = null
    var onExpandClick: (() -> Unit)? = null
    
    private var initialWidth = 0
    private var initialBottomMargin = 0
    
    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        clipToPadding = false
        clipChildren = false
        
        post {
            initialWidth = width
            initialBottomMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        }
        
        binding.card.setOnClickListener {
            if (!isExpanded) {
                onExpandClick?.invoke()
            }
        }
        
        binding.closeButton.setOnClickListener {
            onCloseClick?.invoke()
        }
    }
    
    fun setButtonTitle(title: String) {
        binding.buttonText.text = title
    }
    
    fun setExpandedTitle(title: String) {
        binding.titleText.text = title
    }
    
    fun setSubtitle(subtitle: String) {
        binding.subtitleText.text = subtitle
    }
    
    fun getContentContainer(): FrameLayout {
        return binding.contentContainer
    }
    
    fun expand(hideOtherCard: View? = null, moveToBottom: Boolean = true) {
        if (isExpanded) return
        
        hideOtherCard?.visibility = View.GONE
        
        if (moveToBottom) {
            val targetMargin = 32.dpToPx()
            val currentMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
            val marginAnimator = ValueAnimator.ofInt(currentMargin, targetMargin)
            marginAnimator.addUpdateListener { animation ->
                val params = layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = animation.animatedValue as Int
                layoutParams = params
            }
            marginAnimator.duration = 300
            marginAnimator.start()
        }
        
        expandCard()
        isExpanded = true
    }
    
    fun collapse(showOtherCard: View? = null, targetMarginBottom: Int = 80.dpToPx()) {
        if (!isExpanded) return
        
        collapseCard()
        isExpanded = false
        
        postDelayed({
            showOtherCard?.visibility = View.VISIBLE
        }, 450)
    }
    
    fun adjustHeightForContent() {
        if (!isExpanded) return
        
        binding.expandedContent.requestLayout()
        binding.expandedContent.forceLayout()
        
        val screenWidth = resources.displayMetrics.widthPixels
        val targetWidth = screenWidth - (24.dpToPx() * 2)
        
        binding.expandedContent.measure(
            View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val newContentHeight = binding.expandedContent.measuredHeight
        
        val currentHeight = height
        val newHeight = binding.buttonText.measuredHeight + (20.dpToPx() * 2) + newContentHeight + 40.dpToPx()
        
        if (newHeight != currentHeight) {
            val heightAnimator = ValueAnimator.ofInt(currentHeight, newHeight)
            heightAnimator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val params = layoutParams
                params.height = value
                layoutParams = params
            }
            heightAnimator.duration = 200
            heightAnimator.interpolator = DecelerateInterpolator()
            heightAnimator.start()
        }
    }
    
    private fun expandCard() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetWidth = screenWidth - (24.dpToPx() * 2)
        
        binding.expandedContent.measure(
            View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetContentHeight = binding.expandedContent.measuredHeight
        
        binding.expandedContent.visibility = View.VISIBLE
        binding.expandedContent.alpha = 0f
        
        val currentWidth = width
        val currentHeight = height
        val targetHeight = currentHeight + targetContentHeight
        
        val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
        widthAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.width = value
            params.marginStart = (screenWidth - value) / 2
            params.marginEnd = (screenWidth - value) / 2
            layoutParams = params
        }
        widthAnimator.duration = 400
        widthAnimator.interpolator = DecelerateInterpolator()
        
        val heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight)
        heightAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val params = layoutParams
            params.height = value
            layoutParams = params
        }
        heightAnimator.duration = 400
        heightAnimator.interpolator = DecelerateInterpolator()
        
        val fadeOutButton = ObjectAnimator.ofFloat(binding.buttonText, View.ALPHA, 1f, 0f)
        fadeOutButton.duration = 200
        fadeOutButton.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.buttonText.visibility = View.GONE
            }
        })
        
        val fadeInContent = ObjectAnimator.ofFloat(binding.expandedContent, View.ALPHA, 0f, 1f)
        fadeInContent.duration = 300
        fadeInContent.startDelay = 200
        fadeInContent.interpolator = DecelerateInterpolator()
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(widthAnimator, heightAnimator, fadeOutButton, fadeInContent)
        animatorSet.start()
    }
    
    private fun collapseCard() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetWidth = initialWidth
        
        binding.buttonText.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = binding.buttonText.measuredHeight + (20.dpToPx() * 2)
        
        val currentWidth = width
        val currentHeight = height
        
        val fadeOutContent = ObjectAnimator.ofFloat(binding.expandedContent, View.ALPHA, 1f, 0f)
        fadeOutContent.duration = 200
        fadeOutContent.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.expandedContent.visibility = View.GONE
            }
        })
        
        binding.buttonText.visibility = View.VISIBLE
        val fadeInButton = ObjectAnimator.ofFloat(binding.buttonText, View.ALPHA, 0f, 1f)
        fadeInButton.duration = 300
        fadeInButton.startDelay = 150
        
        val widthAnimator = ValueAnimator.ofInt(currentWidth, targetWidth)
        widthAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.width = value
            params.marginStart = (screenWidth - value) / 2
            params.marginEnd = (screenWidth - value) / 2
            layoutParams = params
        }
        widthAnimator.duration = 400
        widthAnimator.interpolator = DecelerateInterpolator()
        widthAnimator.startDelay = 150
        
        val heightAnimator = ValueAnimator.ofInt(currentHeight, targetHeight)
        heightAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val params = layoutParams
            params.height = value
            layoutParams = params
        }
        heightAnimator.duration = 400
        heightAnimator.interpolator = DecelerateInterpolator()
        heightAnimator.startDelay = 150
        
        val marginAnimator = ValueAnimator.ofInt(0, initialBottomMargin)
        marginAnimator.addUpdateListener { animation ->
            val params = layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = animation.animatedValue as Int
            layoutParams = params
        }
        marginAnimator.duration = 300
        marginAnimator.startDelay = 300
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeOutContent, fadeInButton, widthAnimator, heightAnimator, marginAnimator)
        animatorSet.start()
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
