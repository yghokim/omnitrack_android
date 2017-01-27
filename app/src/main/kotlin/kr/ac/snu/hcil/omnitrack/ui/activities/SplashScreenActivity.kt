package kr.ac.snu.hcil.omnitrack.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.amazonaws.mobile.AWSMobileClient
import com.amazonaws.mobile.user.IdentityManager
import com.amazonaws.mobile.user.IdentityProvider
import com.amazonaws.mobile.user.signin.SignInManager
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity


/**
 * Created by Young-Ho on 8/1/2016.
 * https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 */
class SplashScreenActivity : Activity() {

    private val LOG_TAG = SplashScreenActivity::class.java.simpleName

    private inner class OmniTrackSignInResultsHandler : IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result for an alraedy signed in user and starts the main
         * activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess(provider: IdentityProvider) {
            Log.d(LOG_TAG, String.format("User sign-in with previous %s provider succeeded",
                    provider.displayName))

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose()

            Toast.makeText(this@SplashScreenActivity, String.format("Sign-in with %s succeeded.",
                    provider.displayName), Toast.LENGTH_LONG).show()

            AWSMobileClient.defaultMobileClient()
                    .identityManager
                    .loadUserInfoAndImage(provider) { goMain() }
        }

        /**
         * For the case where the user previously was signed in, and an attempt is made to sign the
         * user back in again, there is not an option for the user to cancel, so this is overriden
         * as a stub.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        override fun onCancel(provider: IdentityProvider) {
            Log.wtf(LOG_TAG, "Cancel can't happen when handling a previously sign-in user.")
        }

        /**
         * Receives the sign-in result that an error occurred signing in with the previously signed
         * in provider and re-directs the user to the sign-in activity to sign in again.
         * @param provider the identity provider with which the user attempted sign-in.
         * *
         * @param ex the exception that occurred.
         */
        override fun onError(provider: IdentityProvider, ex: Exception) {
            Log.e(LOG_TAG,
                    String.format("Cognito credentials refresh with %s provider failed. Error: %s",
                            provider.displayName, ex.message), ex)

            Toast.makeText(this@SplashScreenActivity, String.format("Sign-in with %s failed.",
                    provider.displayName), Toast.LENGTH_LONG).show()
            //goSignIn()
            goMain()
        }
    }

    private lateinit var signInManager: SignInManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (false/*BuildConfig.DEBUG*/) {
            goMain()
        } else {
            val thread = Thread(Runnable {
                signInManager = SignInManager.getInstance(applicationContext)

                val provider = signInManager.previouslySignedInProvider

                // if the user was already previously in to a provider.
                if (provider != null) {
                    // asynchronously handle refreshing credentials and call our handler.
                    signInManager.refreshCredentialsWithProvider(this@SplashScreenActivity,
                            provider, OmniTrackSignInResultsHandler())
                } else {
                    // Asyncronously go to the main activity (after the splash delay has expired).
                    goSignIn()
                }
            })
            thread.start()
        }
    }

    private fun goMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goSignIn() {
        Log.d(LOG_TAG, "Launching Sign-in Activity...")

        //val intent = Intent(this, SignInActivity::class.java)
        val intent = Intent(this, ExperimentSignInActivity::class.java)

        startActivity(intent)
        finish()
    }
}