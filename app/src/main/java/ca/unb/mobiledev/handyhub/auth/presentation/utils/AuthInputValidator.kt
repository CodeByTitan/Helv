package ca.unb.mobiledev.handyhub.auth.presentation.utils

import android.util.Patterns

object AuthInputValidator {
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPhone(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }
    
    fun isValidOtp(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }
    
    fun formatPhoneNumber(phone: String): String {
        return "+1$phone"
    }
}

