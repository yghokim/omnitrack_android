package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.amazonaws.activities.SignInActivity
import com.amazonaws.mobile.AWSMobileClient
import com.amazonaws.mobile.user.IdentityManager
import com.amazonaws.mobile.user.IdentityProvider
import com.amazonaws.mobile.user.signin.SignInManager
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by younghokim on 2016. 11. 15..
 */
abstract class OTActivity(val checkRefreshingCredential: Boolean = false) : AppCompatActivity() {

    private val LOG_TAG = "OmniTrackActivity"

    private inner class OmniTrackSignInResultsHandler : IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result for an alraedy signed in user and starts the main
         * activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess(provider: IdentityProvider) {
            /*Log.d(LOG_TAG, String.format("User sign-in with previous %s provider succeeded",
                    provider.displayName))*/

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose()

            /*
            Toast.makeText(this@OTActivity, String.format("Sign-in with %s succeeded.",
                    provider.displayName), Toast.LENGTH_LONG).show()
*/

            ExperimentConsentManager.startProcess(this@OTActivity, AWSMobileClient.defaultMobileClient().syncManager, object : ExperimentConsentManager.ResultListener {
                override fun onConsentApproved() {
                    performSignInProcessCompletelyFinished()
                }

                override fun onConsentFailed() {
                    Log.d(LOG_TAG, "Consent process was failed. go Sign-in.")
                    goSignIn()
                }

                override fun onConsentDenied() {
                    Log.d(LOG_TAG, "Consent was denied by user. go Sign-in.")
                    goSignIn()
                }

            })
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

            Toast.makeText(this@OTActivity, String.format("Sign-in with %s failed.",
                    provider.displayName), Toast.LENGTH_LONG).show()
            //goSignIn()
            performSignInProcessCompletelyFinished()
        }
    }

    private lateinit var signInManager: SignInManager

    protected var isSessionLoggingEnabled = true

    private var resumedAt: Long = 0

    val durationPickers = ArrayList<DurationPicker>()

    private var touchMoveAmount: PointF = PointF()

    protected val creationSubscriptions = CompositeSubscription()

    private val signedInUserSubject: BehaviorSubject<OTUser> = BehaviorSubject.create<OTUser>()

    val signedInUserObservable: rx.Observable<OTUser>
        get() = signedInUserSubject
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())

    private fun refreshCredentialsWithFallbackSignIn() {
        signInManager = SignInManager.getInstance(applicationContext)

        val provider = signInManager.previouslySignedInProvider

        // if the user was already previously in to a provider.
        if (provider != null) {
            // asynchronously handle refreshing credentials and call our handler.
            signInManager.refreshCredentialsWithProvider(this@OTActivity,
                    provider, OmniTrackSignInResultsHandler())
        } else {
            // Asyncronously go to the main activity (after the splash delay has expired).
            Log.d(LOG_TAG, "Couldn't find previously-signed in provider.")
            goSignInUnlessUserCached()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (false/*BuildConfig.DEBUG*/) {
            performSignInProcessCompletelyFinished()
        } else {
            if (checkRefreshingCredential) {
                val thread = Thread(Runnable {
                    refreshCredentialsWithFallbackSignIn()
                })
                thread.start()
            } else {
                goSignInUnlessUserCached()
            }
        }
    }

    private fun goSignInUnlessUserCached() {
        creationSubscriptions.add(
                OTApplication.app.currentUserObservable.subscribe({
                    user ->
                    signedInUserSubject.onNext(user)

                    if (!checkRefreshingCredential) {
                        //background sign in
                        val ignoreFlag = intent.getBooleanExtra(OTApplication.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, false)
                        println("OMNITRACK ignore flag: ${ignoreFlag}, current activity: ${this.localClassName}")
                        if (!ignoreFlag) {
                            val thread = Thread(Runnable {
                                val identityManager = AWSMobileClient.defaultMobileClient().identityManager
                                identityManager.loginWithProviderSilently(
                                        identityManager.currentIdentityProvider,
                                        object : IdentityManager.SignInResultsHandler {
                                            override fun onCancel(provider: IdentityProvider?) {

                                            }

                                            override fun onError(provider: IdentityProvider?, ex: Exception?) {

                                            }

                                            override fun onSuccess(provider: IdentityProvider) {
                                                identityManager.getUserID(object : IdentityManager.IdentityHandler {
                                                    override fun handleError(exception: Exception?) {

                                                    }

                                                    override fun handleIdentityID(identityId: String) {
                                                        if (user.objectId == identityId) {
                                                            Toast.makeText(this@OTActivity, "Background sign in check was successful.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Log.e("OMNITRACK", "Something wrong with your identity id.")
                                                            Toast.makeText(this@OTActivity, "Signed-in user account id is different from what is stored. Please re-sign in.", Toast.LENGTH_SHORT).show()
                                                            goSignIn()
                                                        }
                                                    }

                                                })
                                            }

                                        }
                                )
                            })
                            thread.start()
                        }
                    }

                    onSignInProcessCompletelyFinished()
                })
                {
                    e ->
                    Log.d(LOG_TAG, "User is not stored in device. go Sign-in.")
                    goSignIn()
                })
    }

    private fun goSignIn() {
        Log.d(LOG_TAG, "Launching Sign-in Activity...")

        val intent = Intent(this, SignInActivity::class.java)
        //val intent = Intent(this, ExperimentSignInActivity::class.java)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ExperimentConsentManager.handleActivityResult(false, requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()

        resumedAt = System.currentTimeMillis()


    }

    override fun onDestroy() {
        super.onDestroy()
        durationPickers.clear()
        creationSubscriptions.clear()
    }

    override fun onPause() {
        super.onPause()

        if (isSessionLoggingEnabled) {
            val from = if (intent.hasExtra(OTApplication.INTENT_EXTRA_FROM)) {
                intent.getStringExtra(OTApplication.INTENT_EXTRA_FROM)
            } else null

            val contentObject = JsonObject()
            contentObject.addProperty("isFinishing", isFinishing)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            OTApplication.logger.writeSessionLog(this, now - resumedAt, now, from, contentObject.toString())
        }
    }

    private fun performSignInProcessCompletelyFinished() {
        println("OMNITRACK loading user from the application global instance")
        creationSubscriptions.add(
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    println("OMNITRACK received user from global instance")
                    this.signedInUserSubject.onNext(user)
                }
        )

        onSignInProcessCompletelyFinished()
    }

    protected open fun onSignInProcessCompletelyFinished() {

    }

    protected open fun onSessionLogContent(contentObject: JsonObject) {
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            touchMoveAmount.x++
            touchMoveAmount.y++
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (touchMoveAmount.x < 10 || touchMoveAmount.y < 10) {
                for (v in durationPickers) {
                    val outRect = Rect()
                    v.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        v.setInputMode(false, true)
                    }
                }
            }
            touchMoveAmount.x = 0f
            touchMoveAmount.y = 0f
        }

        return super.dispatchTouchEvent(event)
    }
}