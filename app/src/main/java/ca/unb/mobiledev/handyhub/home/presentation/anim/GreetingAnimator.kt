package ca.unb.mobiledev.handyhub.home.presentation.anim

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw

class GreetingAnimator {
    companion object {

        fun animateGreetingSlideIn(greetingContainer: View) {
            // Wait until we know measured height + window insets
            greetingContainer.doOnPreDraw {
                val parent = greetingContainer.parent as? ViewGroup
                // Make sure the parent doesn't clip while we start above it (looks more "from top")
                parent?.clipToPadding = false
                parent?.clipChildren = false

                // Get status bar/top inset so we start just above the screen edge
                val topInset = ViewCompat.getRootWindowInsets(greetingContainer)
                    ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0

                val startY = - (greetingContainer.height + topInset).toFloat()

                // Initial state
                greetingContainer.translationY = startY
                greetingContainer.alpha = 0f

                // Slide down with smooth easing - no overshoot
                val slideDown = ObjectAnimator.ofFloat(
                    greetingContainer,
                    View.TRANSLATION_Y,
                    startY,
                    0f
                ).apply {
                    duration = 600
                    interpolator = DecelerateInterpolator() // smooth, gentle landing
                }

                val fadeIn = ObjectAnimator.ofFloat(greetingContainer, View.ALPHA, 0f, 1f).apply {
                    duration = 550
                }

                // Optional: promote to a hardware layer during the animation for smoothness
                greetingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                AnimatorSet().apply {
                    // Start fading slightly after the slide begins for a "revealed" feel
                    playTogether(slideDown, fadeIn)
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            greetingContainer.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                    })
                    start()
                }
            }
        }

        fun animateSearchBarExpand(searchBarContainer: View) {
            // Ensure we know measured width/height
            searchBarContainer.doOnPreDraw {
                // Start fully transparent, no scaling
                searchBarContainer.alpha = 0f
                searchBarContainer.scaleX = 1f
                searchBarContainer.scaleY = 1f
                searchBarContainer.translationX = 0f

                // Outline-based left->right reveal (API 21+)
                class RevealOutlineProvider : ViewOutlineProvider() {
                    var fraction: Float = 0f
                    override fun getOutline(view: View, outline: Outline) {
                        val w = (view.width * fraction).toInt().coerceIn(0, view.width)
                        outline.setRect(0, 0, w, view.height)
                    }
                }
                val outlineProvider = RevealOutlineProvider()
                searchBarContainer.outlineProvider = outlineProvider
                searchBarContainer.clipToOutline = true

                // Animate the reveal fraction from 0 -> 1
                val reveal = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 650
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { va ->
                        outlineProvider.fraction = va.animatedValue as Float
                        searchBarContainer.invalidateOutline()
                    }
                }

                // Subtle overall fade so it doesn't just "pop"
                val fade = ObjectAnimator.ofFloat(searchBarContainer, View.ALPHA, 0f, 1f).apply {
                    duration = 500
                    startDelay = 100 // reveal starts, then opacity catches up
                    interpolator = DecelerateInterpolator()
                }

                // Optional: small "from left" presence without scaling
                // (comment out if you don't want the nudge)
                val nudge = ObjectAnimator.ofFloat(searchBarContainer, View.TRANSLATION_X, -8f, 0f).apply {
                    duration = 650
                    interpolator = DecelerateInterpolator()
                }

                // Hardware layer for smoothness during animation
                searchBarContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                AnimatorSet().apply {
                    playTogether(reveal, fade, nudge)
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Clean up (optional): keep clipping off after reveal
                            searchBarContainer.clipToOutline = false
                            searchBarContainer.outlineProvider = ViewOutlineProvider.BACKGROUND
                            searchBarContainer.setLayerType(View.LAYER_TYPE_NONE, null)
                        }
                    })
                    start()
                }
            }
        }

        fun animateBottomNavigationSlideUp(bottomNavContainer: View) {
            // (unchanged)
            bottomNavContainer.translationY = 200f
            bottomNavContainer.alpha = 0f

            val slideUp = ObjectAnimator.ofFloat(bottomNavContainer, View.TRANSLATION_Y, 200f, 0f).apply {
                duration = 900
            }
            val fadeIn = ObjectAnimator.ofFloat(bottomNavContainer, View.ALPHA, 0f, 1f).apply {
                duration = 900
            }

            AnimatorSet().apply {
                playTogether(slideUp, fadeIn)
                start()
            }
        }
    }
}
