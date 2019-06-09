package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import kotlinx.android.synthetic.main.activity_my_account.*
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity

class MyAccountActivity : MultiButtonActionBarActivity(R.layout.activity_my_account), View.OnClickListener {

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        ui_text_email.text = authManager.email

        creationSubscriptions.add(
                authManager.authTokenChanged.subscribe {
                    ui_text_email.text = authManager.email
                }
        )

        ui_btn_logout.setOnClickListener(this)
        ui_btn_change_email.setOnClickListener(this)
        ui_btn_change_password.setOnClickListener(this)
        ui_btn_drop_experiment.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            ui_btn_logout -> {
                DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this, null, getString(R.string.msg_profile_unlink_account_confirm), R.string.msg_logout, onYes = {
                    eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_SIGNED_OUT)
                    this.creationSubscriptions.add(
                            authManager.signOut().subscribe()
                    )
                }).show()
            }
            ui_btn_change_email -> {
                DialogHelper.makeValidationTextInputDialog(this,
                        "Change E-mail",
                        "New E-mail address",
                        { email ->
                            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                null
                            } else getString(R.string.error_invalid_email)
                        },
                        { email -> authManager.updateEmail(email) }
                ).show()
            }

            ui_btn_change_password -> {
                DialogHelper.makePasswordResetDialog(this,
                        { password -> authManager.validatePassword(password) },
                        { original, newPassword ->
                            authManager.changePassword(original, newPassword)
                        }).show()
            }
        }
    }

}