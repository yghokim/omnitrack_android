package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.VersionCheckDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.util.*

/**
 * Created by younghokim on 2016. 11. 15..
 */
abstract class OTActivity(val checkRefreshingCredential: Boolean = false, val checkUpdateAvailable: Boolean = true) : AppCompatActivity() {

    private val LOG_TAG = "OmniTrackActivity"

    companion object {
        val intentFilter: IntentFilter by lazy { IntentFilter(OTApplication.BROADCAST_ACTION_NEW_VERSION_DETECTED) }
    }

    private inner class OmniTrackSignInResultsHandler : OTAuthManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result for an alraedy signed in user and starts the main
         * activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess() {

            ExperimentConsentManager.startProcess(this@OTActivity, OTAuthManager.userId!!, object : ExperimentConsentManager.ResultListener {
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
        override fun onCancel() {
            Log.wtf(LOG_TAG, "Cancel can't happen when handling a previously sign-in user.")
        }

        /**
         * Receives the sign-in result that an error occurred signing in with the previously signed
         * in provider and re-directs the user to the sign-in activity to sign in again.
         * @param provider the identity provider with which the user attempted sign-in.
         * *
         * @param ex the exception that occurred.
         */
        override fun onError(e: Throwable) {
            //goSignIn()
            performSignInProcessCompletelyFinished()
        }
    }

    protected var isSessionLoggingEnabled = true

    private var resumedAt: Long = 0

    val durationPickers = ArrayList<DurationPicker>()

    private var touchMoveAmount: PointF = PointF()

    protected val creationSubscriptions = CompositeSubscription()

    private val signedInUserSubject: BehaviorSubject<OTUser> = BehaviorSubject.create<OTUser>()

    private var backgroundSignInCheckThread: Thread? = null

    private val broadcastReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == OTApplication.BROADCAST_ACTION_NEW_VERSION_DETECTED) {
                    val versionName = intent.getStringExtra(OTApplication.INTENT_EXTRA_LATEST_VERSION_NAME)
                    VersionCheckDialogFragment.makeInstance(versionName)
                            .show(supportFragmentManager, "VersionCheck")

                    OTApplication.app.systemSharedPreferences.edit()
                            .putString(OTVersionCheckService.PREF_LAST_NOTIFIED_VERSION, versionName)
                            .apply()
                }
            }

        }
    }

    val signedInUserObservable: rx.Observable<OTUser>
        get() = signedInUserSubject
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    private fun refreshCredentialsWithFallbackSignIn() {
        if (OTAuthManager.isUserSignedIn() && NetworkHelper.isConnectedToInternet()) {
            //println("${LOG_TAG} OMNITRACK Google is signed in.")
            if (NetworkHelper.isConnectedToInternet()) {
                DatabaseManager.refreshInstanceIdToServerIfExists(true)
                OTAuthManager.refreshCredentialWithFallbackSignIn(this, OmniTrackSignInResultsHandler())
            }
        } else {
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

        PreferenceManager.setDefaultValues(this, R.xml.global_preferences, false)
    }

    protected fun getUserOrGotoSignIn(): Single<OTUser> {
        return OTApplication.app.currentUserObservable.toSingle().doOnError {
            goSignIn()
        }
    }

    private fun goSignInUnlessUserCached() {
        println("OMNITRACK Check whether user is cached. Otherwise, go to sign in")
        creationSubscriptions.add(
                OTApplication.app.currentUserObservable.subscribe({
                    user ->
                    signedInUserSubject.onNext(user)

                    if (!checkRefreshingCredential) {
                        //background sign in
                        val ignoreFlag = intent.getBooleanExtra(OTApplication.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, false)
                        println("OMNITRACK ignore flag: ${ignoreFlag}, current activity: ${this.localClassName}")
                        /*
                        if (!ignoreFlag) {
                            if (backgroundSignInCheckThread?.isAlive ?: false == false) {
                                backgroundSignInCheckThread = Thread(Runnable {
                                    OTAuthManager.refreshCredentialSilently(false, object : OTAuthManager.SignInResultsHandler {
                                        override fun onCancel() {
                                            backgroundSignInCheckThread = null
                                        }

                                        override fun onError(e: Throwable) {
                                            Log.e("OMNITRACK", "RefreshCredential error")
                                            //Toast.makeText(this@OTActivity, "Background sign in check failed.", Toast.LENGTH_SHORT).show()
                                            e.printStackTrace()

                                            backgroundSignInCheckThread = null
                                        }

                                        override fun onSuccess() {
                                            if (user.objectId == OTAuthManager.userId) {
                                                //Toast.makeText(this@OTActivity, "Background sign in check was successful.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Log.e("OMNITRACK", "Something wrong with your identity id.")
                                                //Toast.makeText(this@OTActivity, "Signed-in user account id is different from what is stored. Please re-sign in.", Toast.LENGTH_SHORT).show()
                                                goSignIn()
                                            }

                                            backgroundSignInCheckThread = null
                                        }
                                    }
                                    )
                                })
                                backgroundSignInCheckThread?.start()
                            } else {
                                println("OMNITRACK background sign in of former activity is already in progress. skip current activitiy's.")
                            }
                        }
                        */
                    }

                    onSignInProcessCompletelyFinished()
                },
                {
                    e ->
                    Log.d(LOG_TAG, "User is not stored in device. go Sign-in. ${e.message}")
                    goSignIn()
                })
        )
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

    fun performOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        runOnUiThread {
            onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()

        resumedAt = System.currentTimeMillis()

        if (checkUpdateAvailable) {
            registerReceiver(broadcastReceiver, intentFilter)
        }
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

            val contentObject = Bundle()
            contentObject.putBoolean("isFinishing", isFinishing)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            EventLoggingManager.logSession(this, now - resumedAt, now, from, contentObject)
        }

        if (checkUpdateAvailable) {
            unregisterReceiver(broadcastReceiver)
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

    protected open fun onSessionLogContent(contentObject: Bundle) {
    }

    override fun attachBaseContext(newBase: Context?) {

        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
        /*
        if (Build.VERSION.SDK_INT > 19) {
            super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
        } else {
            super.attachBaseContext(newBase)
        }*/
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