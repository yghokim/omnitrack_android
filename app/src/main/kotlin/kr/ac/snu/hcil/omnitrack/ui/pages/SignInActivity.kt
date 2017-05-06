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
import com.badoo.mobile.util.WeakHandler
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.tbruyelle.rxpermissions.RxPermissions
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import rx.internal.util.SubscriptionList

class SignInActivity : AppCompatActivity() {

    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above.  */
    private var googleOnClickListener: View.OnClickListener? = null

    private lateinit var googleLoginButton: View
    private lateinit var loginInProgressIndicator: View

    private val creationSubscription = SubscriptionList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        googleLoginButton = findViewById(R.id.g_login_button)
        loginInProgressIndicator = findViewById(R.id.ui_loading_indicator)

        // Initialize sign-in buttons.
        googleOnClickListener = OTAuthManager.initializeSignInButton(googleLoginButton, SignInResultsHandler())

        if (googleOnClickListener != null) {
            // if the onClick listener was null, initializeSignInButton will have removed the view.
            this.googleLoginButton.setOnClickListener(View.OnClickListener { view ->
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
                                        this.findViewById(kr.ac.snu.hcil.omnitrack.R.id.g_login_button).callOnClick()
                                    } else {
                                        Log.i(LOG_TAG, "Permissions not granted for Google sign-in. :(")
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

                    // call the Google onClick listener.
                    googleOnClickListener!!.onClick(view)
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
        WeakHandler().post {
            this.googleLoginButton.visibility = View.GONE
            this.loginInProgressIndicator.visibility = View.VISIBLE
        }
    }

    private fun toIdleMode() {
        WeakHandler().post {
            this.googleLoginButton.visibility = View.VISIBLE
            this.loginInProgressIndicator.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        OTAuthManager.handleActivityResult(requestCode, resultCode, data)
        ExperimentConsentManager.handleActivityResult(true, requestCode, resultCode, data)
    }

    private fun goHomeActivity() {
        Log.d(LOG_TAG, "Launching Main Activity...");
        startActivity(Intent(this@SignInActivity, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(OTApplication.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true))
        // finish should always be called on the main thread.
        finish();
    }

    override fun onDestroy() {
        super.onDestroy()
        ExperimentConsentManager.finishProcess()
        creationSubscription.clear()
    }

    /**
     * SignInResultsHandler handles the final result from sign in. Making it static is a best
     * practice since it may outlive the SplashActivity's life span.
     */
    private inner class SignInResultsHandler : OTAuthManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess() {
            Log.d(LOG_TAG, String.format("User sign-in with Google succeeded"))

            Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_succeeded)), Toast.LENGTH_LONG).show()

            ExperimentConsentManager.startProcess(this@SignInActivity, OTAuthManager.userId!!, object : ExperimentConsentManager.ResultListener {
                    override fun onConsentApproved() {
                        val activatedRef = DatabaseManager.currentUserRef?.child("examples_generated")
                        activatedRef?.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError?) {
                                goHomeActivity()
                            }

                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists() && snapshot.value == true) {
                                    goHomeActivity()
                                } else {
                                    creationSubscription.add(
                                            OTApplication.app.currentUserObservable.subscribe {
                                                user ->
                                                user.addExampleTrackers()
                                                activatedRef.setValue(true, object : DatabaseReference.CompletionListener {
                                                    override fun onComplete(error: DatabaseError?, ref: DatabaseReference?) {
                                                        if (error == null) {
                                                            goHomeActivity()
                                                        }
                                                    }

                                                })
                                            }
                                    )
                                }


                            }

                        }) ?: goHomeActivity()
                    }

                    override fun onConsentFailed() {
                        println("consent failed")
                        toIdleMode()
                    }

                    override fun onConsentDenied() {
                        println("consent was denied")
                        toIdleMode()
                    }

            })
        }

        /**
         * Receives the sign-in result indicating the user canceled and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        override fun onCancel() {
            Log.d(LOG_TAG, String.format("User sign-in with Google canceled."))
            Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_canceled)), Toast.LENGTH_LONG).show()

            toIdleMode()
        }

        /**
         * Receives the sign-in result that an error occurred signing in and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         * *
         * @param ex the exception that occurred.
         */
        override fun onError(e: Throwable) {
            Log.e(LOG_TAG, String.format("User Sign-in failed for Google : %s", e.message), e)

            val errorDialogBuilder = AlertDialog.Builder(this@SignInActivity)
            errorDialogBuilder.setTitle("Sign-In Error")
            errorDialogBuilder.setMessage(
                    String.format("Sign-in with Google failed.\n%s", e.message))
            errorDialogBuilder.setNeutralButton("Ok", null)
            errorDialogBuilder.show()

            toIdleMode()
        }
    }

    companion object {
        private val LOG_TAG = SignInActivity::class.java.simpleName
        /**
         * Permission Request Code (Must be < 256).
         */
        private val GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93
    }
}