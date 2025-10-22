package ca.unb.mobiledev.handyhub.auth.presentation.onboarding_steps

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ca.unb.mobiledev.handyhub.auth.presentation.OnboardingActivity
import ca.unb.mobiledev.handyhub.auth.domain.viewmodel.AuthViewModel
import ca.unb.mobiledev.handyhub.auth.presentation.utils.AuthInputValidator
import ca.unb.mobiledev.handyhub.databinding.FragmentDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class DetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private var selectedDateOfBirth: String = ""
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInputValidation()
        setupDatePicker()
        setupContinueButton()
        observeUpdateDetailsState()
    }
    
    private fun setupInputValidation() {
        binding.nameInput.addTextChangedListener(createValidationWatcher {
            binding.nameInputLayout.error = null
            updateContinueButtonState()
        })
        
        binding.emailInput.addTextChangedListener(createValidationWatcher {
            val email = binding.emailInput.text.toString()
            binding.emailInputLayout.error = if (email.isNotEmpty() && !AuthInputValidator.isValidEmail(email)) {
                "Invalid email"
            } else {
                null
            }
            updateContinueButtonState()
        })
    }
    
    private fun setupDatePicker() {
        binding.dobInput.setOnClickListener {
            showDatePickerDialog()
        }
        
        binding.dobInputLayout.setEndIconOnClickListener {
            showDatePickerDialog()
        }
    }
    
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR) - 15
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                
                if (isAgeValid(selectedCalendar)) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    selectedDateOfBirth = dateFormat.format(selectedCalendar.time)
                    binding.dobInput.setText(selectedDateOfBirth)
                    binding.dobInputLayout.error = null
                } else {
                    binding.dobInput.setText("")
                    binding.dobInputLayout.error = "You must be at least 15 years old"
                }
                updateContinueButtonState()
            },
            year,
            month,
            day
        )
        
        val minAgeCalendar = Calendar.getInstance()
        minAgeCalendar.add(Calendar.YEAR, -15)
        datePickerDialog.datePicker.maxDate = minAgeCalendar.timeInMillis
        datePickerDialog.show()
    }
    
    private fun isAgeValid(selectedDate: Calendar): Boolean {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR)
        
        if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        return age >= 15
    }
    
    private fun setupContinueButton() {
        binding.buttonContinue.isEnabled = false
        binding.buttonContinue.setOnClickListener {
            if (validateAllFields()) {
                saveDetailsAndNavigate()
            }
        }
    }
    
    private fun validateAllFields(): Boolean {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val dob = binding.dobInput.text.toString().trim()
        
        var isValid = true
        
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        }
        
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!AuthInputValidator.isValidEmail(email)) {
            binding.emailInputLayout.error = "Invalid email"
            isValid = false
        }
        
        if (dob.isEmpty()) {
            binding.dobInputLayout.error = "Date of birth is required"
            isValid = false
        }
        
        return isValid
    }
    
    private fun updateContinueButtonState() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val dob = binding.dobInput.text.toString().trim()
        
        binding.buttonContinue.isEnabled = name.isNotEmpty() && 
                email.isNotEmpty() && 
                AuthInputValidator.isValidEmail(email) &&
                dob.isNotEmpty()
    }
    
    private fun saveDetailsAndNavigate() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val dob = selectedDateOfBirth
        
        binding.buttonContinue.isEnabled = false
        binding.buttonContinue.text = "Saving..."
        
        authViewModel.updateUserDetailsAndCompleteOnboarding(name, email, dob)
    }
    
    private fun observeUpdateDetailsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.updateDetailsState.collect { resource ->
                when (resource) {
                    is ca.unb.mobiledev.handyhub.util.Resource.Success -> {
                        // Navigation is handled by OnboardingActivity
                    }
                    is ca.unb.mobiledev.handyhub.util.Resource.Error -> {
                        binding.buttonContinue.isEnabled = true
                        binding.buttonContinue.text = "Continue to the app"
                        android.widget.Toast.makeText(
                            requireContext(),
                            resource.message ?: "Failed to save details",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ca.unb.mobiledev.handyhub.util.Resource.Loading -> {}
                }
            }
        }
    }
    
    private fun createValidationWatcher(onTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onTextChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
