package kr.ac.snu.hcil.omnitrack.ui.pages

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.GoogleApiAvailability
import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_sign_in.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import javax.inject.Inject

class SignInActivity : AppCompatActivity() {

    @Inject
    protected lateinit var authManager: OTAuthManager
    @Inject
    protected lateinit var consentManager: ExperimentConsentManager

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    private val creationSubscription = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as OTApp).currentConfiguredContext.configuredAppComponent.inject(this)

        setContentView(R.layout.activity_sign_in)

        val task = GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        task.addOnCompleteListener {
            ui_login_group_switcher.visibility = View.VISIBLE
            g_login_button.setOnClickListener(View.OnClickListener { view ->
                toBusyMode()
                val thisActivity = this@SignInActivity

                if (ContextCompat.checkSelfPermission(thisActivity,
                                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {

                    DialogHelper.makeYesNoDialogBuilder(this, getString(R.string.msg_contacts_permission_required),
                            getString(R.string.msg_contacts_permission_rationale),
                            yesLabel = R.string.msg_allow_permission, noLabel = R.string.msg_cancel, onYes = {
                        val permissions = RxPermissions(this)
                        permissions.request(Manifest.permission.GET_ACCOUNTS)
                                .subscribe {
                                    granted ->
                                    if (granted) {
                                        this.findViewById<View>(kr.ac.snu.hcil.omnitrack.R.id.g_login_button).callOnClick()
                                    } else {
                                        Toast.makeText(this, getString(R.string.msg_contacts_permission_denied_message), Toast.LENGTH_LONG).show()
                                        toIdleMode()
                                    }
                                }
                    }, onNo = {
                        toIdleMode()
                    }, onCancel = {
                        toIdleMode()
                    }, cancelable = false).show()
                    return@OnClickListener
                } else {
                    creationSubscription.add(
                            authManager.startSignInProcess(this)
                                    .doOnSuccess {
                                        if (it) {
                                            this@SignInActivity.runOnUiThread {
                                                Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_succeeded)), Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Log.d(LOG_TAG, String.format("User sign-in with Google canceled."))
                                            this@SignInActivity.runOnUiThread {
                                                Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_canceled)), Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    .flatMap { approved ->
                                        consentManager.startProcess(this@SignInActivity).doOnSuccess {
                                            if (it) {
                                                eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_CONSENT_APPROVED)
                                            } else {
                                                eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_CONSENT_DENIED)
                                            }
                                        }
                                    }
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ approved ->
                                        if (approved) {
                                            goHomeActivity()
                                        } else {
                                            toIdleMode()
                                        }
                                    }, { e ->
                                        val errorDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                        errorDialogBuilder.setTitle("Sign-In Error")
                                        errorDialogBuilder.setMessage(
                                                String.format("Sign-in Process failed.\n%s", e.message))
                                        errorDialogBuilder.setNeutralButton("Ok", null)
                                        errorDialogBuilder.show()

                                        toIdleMode()
                                    })
                    )
                }
            })
        }

        /*
        signInManager.initializeSignInButton(CognitoUserPoolsSignInProvider.class,
                this.findViewById(R.id.signIn_imageButton_login));*/
    }

    override fun onStart() {
        super.onStart()
        toIdleMode()
    }

    private fun toBusyMode() {
        runOnUiThread {
            g_login_button.visibility = View.GONE
            ui_loading_indicator.visibility = View.VISIBLE
        }
    }

    private fun toIdleMode() {
        runOnUiThread {
            g_login_button.visibility = View.VISIBLE
            ui_loading_indicator.visibility = View.GONE
        }
    }

    private fun goHomeActivity() {
        eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_SIGNED_IN)
        Log.d(LOG_TAG, "Launching Main Activity...")
        startActivity(Intent(this@SignInActivity, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(OTApp.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true)
                .putExtra(HomeActivity.INTENT_EXTRA_INITIAL_LOGIN, true)
        )
        // finish should always be called on the main thread.
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        creationSubscription.clear()
    }

    companion object {
        private val LOG_TAG = SignInActivity::class.java.simpleName
        /**
         * Permission Request Code (Must be < 256).
         */
        private val GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93
    }
}