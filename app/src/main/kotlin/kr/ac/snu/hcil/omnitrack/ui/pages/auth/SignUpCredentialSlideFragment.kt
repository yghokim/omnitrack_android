package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.trimmedLength
import kotlinx.android.synthetic.main.slide_signup_credential.view.*
import kr.ac.snu.hcil.android.common.view.applyTint
import kr.ac.snu.hcil.android.common.view.text.LowercaseInputFilter
import kr.ac.snu.hcil.android.common.view.text.NoBlankInputFilter
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Inject

class SignUpCredentialSlideFragment : SignUpActivity.SlideFragment(SignUpActivity.ESlide.CREDENTIAL_FORM), TextWatcher {

    @Inject
    protected lateinit var authManager: OTAuthManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.slide_signup_credential, container, false)

        if (BuildConfig.DEFAULT_EXPERIMENT_ID == null) {
            view.ui_form_invitation_code.visibility = View.GONE
            view.ui_description.text = getString(R.string.msg_auth_description_email_researcher)
            view.ui_description.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPointed_Light))
        }

        val checkDrawable = applyTint(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_black_24dp)!!, R.color.colorPointed)

        view.ui_textfield_username.filters += arrayOf(LowercaseInputFilter(), NoBlankInputFilter())
        view.ui_textfield_username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val invalidMessage = authManager.validateUsername(s?.toString() ?: "")
                if (invalidMessage == null) {
                    view.ui_form_username.error = null
                    view.ui_form_username.endIconDrawable = checkDrawable
                } else {
                    view.ui_form_username.endIconDrawable = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        view.ui_textfield_email.filters += NoBlankInputFilter()
        view.ui_textfield_email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (Patterns.EMAIL_ADDRESS.matcher(s?.toString() ?: "").matches()) {
                    view.ui_form_email.error = null
                    view.ui_form_email.endIconDrawable = checkDrawable
                } else {
                    view.ui_form_email.endIconDrawable = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })

        view.ui_textfield_invitation_code.filters += NoBlankInputFilter()
        view.ui_textfield_invitation_code.setOnEditorActionListener { v, actionId, event ->
            onNextTried()
            false
        }


        return view
    }

    override fun onNextTried() {
        val usernameInput = view?.ui_textfield_username?.text?.toString() ?: ""
        val emailInput = view?.ui_textfield_email?.text?.toString() ?: ""
        val passwordInput = view?.ui_textfield_password?.text?.toString() ?: ""
        val confirmPasswordInput = view?.ui_textfield_confirm_password?.text?.toString() ?: ""
        val invitationCodeInput = view?.ui_textfield_invitation_code?.text?.toString() ?: ""

        val usernameInvalidMessage = authManager.validateUsername(usernameInput)
        val emailInvalidMessage = if (Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) null else getString(R.string.msg_auth_invalid_email)
        val passwordInvalidMessage = authManager.validatePassword(passwordInput)
        val confirmPasswordInvalidMessage = if (passwordInput.contentEquals(confirmPasswordInput)) null else getString(R.string.msg_auth_confirm_password_invalid)
        val invitationCodeInvalidMessage = if (invitationCodeInput.trimmedLength() > 0) null else getString(R.string.msg_auth_invitation_code_empty)

        view?.ui_form_username?.error = usernameInvalidMessage
        view?.ui_form_password?.error = passwordInvalidMessage
        view?.ui_form_email?.error = emailInvalidMessage
        view?.ui_form_confirm_password?.error = confirmPasswordInvalidMessage
        view?.ui_form_invitation_code?.error = invitationCodeInvalidMessage

        if (usernameInvalidMessage == null && emailInvalidMessage == null && passwordInvalidMessage == null && confirmPasswordInvalidMessage == null && (BuildConfig.DEFAULT_EXPERIMENT_ID == null || invitationCodeInvalidMessage == null)) {
            viewModel.username = usernameInput
            viewModel.email = emailInput
            viewModel.password = passwordInput
            viewModel.invitationCode = invitationCodeInput
            goNext()
        }
    }


    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

}