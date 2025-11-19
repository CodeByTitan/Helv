package ca.unb.mobiledev.handyhub.home.presentation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ca.unb.mobiledev.handyhub.R
import ca.unb.mobiledev.handyhub.auth.presentation.OnboardingActivity
import ca.unb.mobiledev.handyhub.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    @Inject
    lateinit var firestore: FirebaseFirestore
    
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }
    
    private val profileOptions: List<Int> = listOf(
        R.id.cardPremium,
        R.id.cardPromotions,
        R.id.cardHelp,
        R.id.cardInviteFriends,
        R.id.cardEarnByHelping,
        R.id.cardManageAccount,
        R.id.cardAbout,
        R.id.cardSignOut
    )
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setupUserInfo()
        setupClickListeners()
        animateProfileOptions()
    }
    
    private fun setupUserInfo() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // First, try to load from SharedPreferences
            val savedName = sharedPreferences.getString(KEY_USER_NAME, null)
            val savedEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
            
            if (savedName != null && savedEmail != null) {
                // Use cached data
                binding.textViewUserName.text = savedName
                binding.textViewUserEmail.text = savedEmail
                Log.d("ProfileFragment", "Loaded user data from SharedPreferences")
            } else {
                // Fallback to FirebaseAuth while fetching from Firestore
                binding.textViewUserEmail.text = user.email ?: ""
                binding.textViewUserName.text = user.displayName ?: "User"
                
                // Fetch from Firestore and save to SharedPreferences
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val doc = firestore.collection("users").document(user.uid).get().await()
                        if (doc.exists()) {
                            val name = doc.getString("name")
                            val email = doc.getString("email")
                            
                            val finalName = name ?: user.displayName ?: "User"
                            val finalEmail = email ?: user.email ?: ""
                            
                            binding.textViewUserName.text = finalName
                            binding.textViewUserEmail.text = finalEmail
                            
                            // Save to SharedPreferences
                            sharedPreferences.edit().apply {
                                putString(KEY_USER_NAME, finalName)
                                putString(KEY_USER_EMAIL, finalEmail)
                                apply()
                            }
                            Log.d("ProfileFragment", "Saved user data to SharedPreferences")
                        } else {
                            // Fallback to FirebaseAuth displayName if Firestore doesn't have it
                            val finalName = user.displayName ?: "User"
                            binding.textViewUserName.text = finalName
                            
                            // Save to SharedPreferences
                            sharedPreferences.edit().apply {
                                putString(KEY_USER_NAME, finalName)
                                putString(KEY_USER_EMAIL, user.email ?: "")
                                apply()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Error fetching user data: ${e.message}", e)
                        // Fallback to FirebaseAuth
                        val finalName = user.displayName ?: "User"
                        binding.textViewUserName.text = finalName
                        
                        // Save to SharedPreferences even if Firestore fetch fails
                        sharedPreferences.edit().apply {
                            putString(KEY_USER_NAME, finalName)
                            putString(KEY_USER_EMAIL, user.email ?: "")
                            apply()
                        }
                    }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.imageViewEdit.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardPremium.setOnClickListener {
            Toast.makeText(requireContext(), "Get Helv Premium", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardPromotions.setOnClickListener {
            Toast.makeText(requireContext(), "Promotions", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Get Support", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardInviteFriends.setOnClickListener {
            Toast.makeText(requireContext(), "Invite Friends", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardEarnByHelping.setOnClickListener {
            Toast.makeText(requireContext(), "Earn by Helping", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardManageAccount.setOnClickListener {
            Toast.makeText(requireContext(), "Manage Helv Account", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardAbout.setOnClickListener {
            Toast.makeText(requireContext(), "About", Toast.LENGTH_SHORT).show()
        }
        
        binding.cardSignOut.setOnClickListener {
            Log.d("ProfileFragment", "Sign out clicked")
            showSignOutConfirmation()
        }
    }
    
    private fun animateProfileOptions() {
        profileOptions.forEachIndexed { index, cardId ->
            val card = binding.root.findViewById<View>(cardId)
            card?.let {
                it.alpha = 0f
                it.translationY = 100f
                it.visibility = View.VISIBLE
                
                it.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay((index * 80).toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }
    
    private fun showSignOutConfirmation() {
        Log.d("ProfileFragment", "Showing sign out confirmation")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_sign_out_confirmation, null)
        
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.buttonConfirmSignOut)?.setOnClickListener {
            Log.d("ProfileFragment", "User confirmed sign out")
            dialog.dismiss()
            signOut()
        }
        
        dialogView.findViewById<View>(R.id.buttonCancel)?.setOnClickListener {
            Log.d("ProfileFragment", "User cancelled sign out")
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun signOut() {
        auth.signOut()
        
        val intent = Intent(requireContext(), OnboardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
