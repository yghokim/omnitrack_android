package com.amazonaws.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v13.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.amazonaws.mobile.AWSConfiguration

import com.amazonaws.mobile.AWSMobileClient
import com.amazonaws.mobile.user.IdentityManager
import com.amazonaws.mobile.user.IdentityProvider
import com.amazonaws.mobile.user.signin.GoogleSignInProvider
import com.amazonaws.mobile.user.signin.SignInManager
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager
import com.amazonaws.mobileconnectors.cognito.Dataset
import com.amazonaws.mobileconnectors.cognito.Record
import com.amazonaws.mobileconnectors.cognito.SyncConflict
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import kr.ac.snu.hcil.omnitrack.OTApplication

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity

class SignInActivity : Activity() {

    val REQUEST_CODE_EXPERIMENT_SIGN_IN = 6550

    private var signInManager: SignInManager? = null
    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above.  */
    private var googleOnClickListener: View.OnClickListener? = null

    private lateinit var googleLoginButton: View
    private lateinit var loginInProgressIndicator: View

    private var syncManager: CognitoSyncManager? = null
    private var experimentDataset: Dataset? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        signInManager = SignInManager.getInstance(this)

        signInManager!!.setResultsHandler(this, SignInResultsHandler())

        googleLoginButton = findViewById(R.id.g_login_button)
        loginInProgressIndicator = findViewById(R.id.ui_loading_indicator)

        // Initialize sign-in buttons.
        googleOnClickListener = signInManager?.initializeSignInButton(GoogleSignInProvider::class.java, googleLoginButton)

        if (googleOnClickListener != null) {
            // if the onClick listener was null, initializeSignInButton will have removed the view.
            this.googleLoginButton.setOnClickListener(View.OnClickListener { view ->
                toBusyMode()
                val thisActivity = this@SignInActivity
                if (ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@SignInActivity,
                            arrayOf(Manifest.permission.GET_ACCOUNTS),
                            GET_ACCOUNTS_PERMISSION_REQUEST_CODE)
                    return@OnClickListener
                }

                // call the Google onClick listener.
                googleOnClickListener!!.onClick(view)
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
        this.googleLoginButton.visibility = View.GONE
        this.loginInProgressIndicator.visibility = View.VISIBLE
    }

    private fun toIdleMode() {
        this.googleLoginButton.visibility = View.VISIBLE
        this.loginInProgressIndicator.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == GET_ACCOUNTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.findViewById(R.id.g_login_button).callOnClick()
            } else {
                Log.i(LOG_TAG, "Permissions not granted for Google sign-in. :(")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        signInManager?.handleActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_EXPERIMENT_SIGN_IN) {
            if (resultCode != RESULT_OK || data == null) {
                //TODO: delete user if possible.
                val userPool = CognitoUserPool(this, AWSConfiguration.AMAZON_COGNITO_USER_POOL_ID, AWSConfiguration.AMAZON_COGNITO_USER_POOL_CLIENT_ID, AWSConfiguration.AMAZON_COGNITO_USER_POOL_CLIENT_SECRET)
                val user = userPool.getUser(AWSMobileClient.defaultMobileClient().identityManager.underlyingProvider.cachedIdentityId)
                user.deleteUserInBackground(object : GenericHandler {
                    override fun onSuccess() {
                        Log.v("OMNITRACK", "removed user.")
                        toIdleMode()
                    }

                    override fun onFailure(exception: Exception) {
                        Log.v("OMNITRACK", "removal failed.")

                        exception.printStackTrace()
                        toIdleMode()
                    }

                })
                AWSMobileClient.defaultMobileClient().identityManager.signOut()
            } else {
                experimentDataset?.put(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_IS_CONSENT_APPROVED, true.toString())
                arrayOf(
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY)
                        .forEach {
                            experimentDataset?.put(it, data.getStringExtra(it))
                        }
                experimentDataset?.synchronize(object : Dataset.SyncCallback {
                    override fun onDatasetDeleted(dataset: Dataset, datasetName: String?): Boolean {
                        return true
                    }

                    override fun onConflict(dataset: Dataset, conflicts: MutableList<SyncConflict>?): Boolean {
                        return true
                    }

                    override fun onSuccess(dataset: Dataset, updatedRecords: MutableList<Record>?) {
                        goHomeActivity()
                    }

                    override fun onFailure(dse: DataStorageException?) {
                    }

                    override fun onDatasetsMerged(dataset: Dataset, datasetNames: MutableList<String>?): Boolean {
                        return true
                    }

                })
            }
        }
    }

    private fun goHomeActivity() {
        Log.d(LOG_TAG, "Launching Main Activity...");
        startActivity(Intent(this@SignInActivity, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        // finish should always be called on the main thread.
        finish();
    }

    /**
     * SignInResultsHandler handles the final result from sign in. Making it static is a best
     * practice since it may outlive the SplashActivity's life span.
     */
    private inner class SignInResultsHandler : IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess(provider: IdentityProvider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s succeeded",
                    provider.displayName))

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose()

            Toast.makeText(this@SignInActivity, String.format("Sign-in with %s succeeded.",
                    provider.displayName), Toast.LENGTH_LONG).show()

            // Load user name and image.
            AWSMobileClient.defaultMobileClient().identityManager.loadUserInfoAndImage(provider) {
                syncManager = AWSMobileClient.defaultMobileClient().syncManager
                println("OMNITRACK: is device registered: ${syncManager?.isDeviceRegistered}")
                experimentDataset = syncManager?.openOrCreateDataset(OTApplication.ACCOUNT_DATASET_EXPERIMENT)
                if (experimentDataset != null) {
                    experimentDataset?.synchronize(object : Dataset.SyncCallback {
                        override fun onDatasetDeleted(dataset: Dataset?, datasetName: String?): Boolean {
                            return false
                        }

                        override fun onConflict(dataset: Dataset?, conflicts: MutableList<SyncConflict>?): Boolean {
                            return false
                        }

                        override fun onSuccess(dataset: Dataset?, updatedRecords: MutableList<Record>?) {
                            val isConsentApproved = experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_IS_CONSENT_APPROVED)
                            Log.v("OmniTrack", "IsConsent Approved : ${isConsentApproved}")
                            if (isConsentApproved == null || isConsentApproved.toBoolean() != true) {
                                println("consent form was not approved or is first-time login.")
                                startActivityForResult(Intent(this@SignInActivity, ExperimentSignInActivity::class.java), REQUEST_CODE_EXPERIMENT_SIGN_IN)
                            } else {
                                println("consent form has been approved.")
                                println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER))
                                println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP))
                                println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION))
                                println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY))

                                goHomeActivity()
                            }
                        }

                        override fun onFailure(dse: DataStorageException?) {
                        }

                        override fun onDatasetsMerged(dataset: Dataset?, datasetNames: MutableList<String>?): Boolean {
                            return false
                        }

                    })

                } else {

                }

                /*
                    Log.d(LOG_TAG, "Launching Main Activity...");
                    startActivity(new Intent(SignInActivity.this, HomeActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    // finish should always be called on the main thread.
                    finish();
                    */
            }
        }

        /**
         * Receives the sign-in result indicating the user canceled and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        override fun onCancel(provider: IdentityProvider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s canceled.",
                    provider.displayName))

            Toast.makeText(this@SignInActivity, String.format("Sign-in with %s canceled.",
                    provider.displayName), Toast.LENGTH_LONG).show()

            toIdleMode()
        }

        /**
         * Receives the sign-in result that an error occurred signing in and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         * *
         * @param ex the exception that occurred.
         */
        override fun onError(provider: IdentityProvider, ex: Exception) {
            Log.e(LOG_TAG, String.format("User Sign-in failed for %s : %s",
                    provider.displayName, ex.message), ex)

            val errorDialogBuilder = AlertDialog.Builder(this@SignInActivity)
            errorDialogBuilder.setTitle("Sign-In Error")
            errorDialogBuilder.setMessage(
                    String.format("Sign-in with %s failed.\n%s", provider.displayName, ex.message))
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