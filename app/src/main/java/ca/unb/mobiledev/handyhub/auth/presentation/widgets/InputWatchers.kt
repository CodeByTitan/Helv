package ca.unb.mobiledev.handyhub.auth.presentation.widgets

import android.text.Editable
import android.text.TextWatcher
import ca.unb.mobiledev.handyhub.auth.presentation.utils.AuthInputValidator
import ca.unb.mobiledev.handyhub.databinding.ContentAuthCardBinding

/**
 * Centralized input watchers for all text fields
 */
object InputWatchers {
    
    fun createPhoneWatcher(onValid: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 10) onValid()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
    
    fun createEmailWatcher(content: ContentAuthCardBinding): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                content.emailInputLayout.error = if (s.isNullOrEmpty() || AuthInputValidator.isValidEmail(s.toString())) {
                    null
                } else {
                    "Invalid email"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
    
    fun createOtpWatcher(content: ContentAuthCardBinding): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                content.buttonContinue.isEnabled = s?.length == 6
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
}

