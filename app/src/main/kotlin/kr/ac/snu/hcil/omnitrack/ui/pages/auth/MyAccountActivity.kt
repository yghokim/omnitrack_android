package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import kotlinx.android.synthetic.main.activity_my_account.*
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.omnitrack.BuildConfig
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

        if (BuildConfig.DEFAULT_EXPERIMENT_ID == null) {
            ui_btn_drop_experiment.visibility = View.GONE
        }
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
                        getString(R.string.msg_auth_change_email),
                        null,
                        getString(R.string.msg_auth_new_email_address),
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

            ui_btn_drop_experiment -> {
                /*
                DialogHelper.makeValidationTextInputDialog(this,
                        getString(R.string.msg_auth_stop_participation),
                        getString(R.string.msg_auth_stop_participation_message),
                        getString(R.string.msg_auth_stop_participation_confirm_email),
                        { text ->
                            if (authManager.email != text) "The E-mail address is wrong." else null
                        },
                        { text ->
                            Completable.complete()
                        }, { text, dialogAction ->
                    if (text != null) {
                        DialogHelper.makeValidationTextInputDialog(this,
                                getString(R.string.msg_auth_stop_participation),
                                getString(R.string.msg_auth_stop_participation_reason),
                                getString(R.string.msg_auth_stop_participation_insert_reason),
                                { null },
                                { reason ->
                                    authManager.dropOutFromStudy(reason)
                                }).show()
                    } else {

                    }
                }).show()*/
                DialogHelper.makeSimpleAlertBuilder(this,
                        getString(R.string.msg_auth_stop_participation_message),
                        getString(R.string.msg_auth_stop_participation),
                        R.string.msg_gotit
                ).show()
            }
        }
    }

}