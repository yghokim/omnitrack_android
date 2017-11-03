package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.*
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.VersionCheckDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2016. 11. 15..
 */
abstract class OTActivity(val checkRefreshingCredential: Boolean = false, val checkUpdateAvailable: Boolean = true) : AppCompatActivity() {

    private val LOG_TAG = "OmniTrackActivity"

    companion object {
        val intentFilter: IntentFilter by lazy { IntentFilter(OTApp.BROADCAST_ACTION_NEW_VERSION_DETECTED) }

        const val INTENT_KEY_LANGUAGE_AT_CREATION = "language_at_creation"
    }

    private inner class OmniTrackSignInResultsHandler : OTAuthManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result for an alraedy signed in user and starts the main
         * activity.
         * @param provider the identity provider used for sign-in.
         */
        override fun onSuccess() {

            consentManager.startProcess(this@OTActivity, authManager.userId!!, object : ExperimentConsentManager.ResultListener {
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

    @Inject
    protected lateinit var consentManager: ExperimentConsentManager
    @Inject
    protected lateinit var authManager: OTAuthManager
    @Inject
    protected lateinit var systemPreferences: SharedPreferences

    protected var isSessionLoggingEnabled = true

    private var resumedAt: Long = 0

    val durationPickers = ArrayList<DurationPicker>()

    private var touchMoveAmount: PointF = PointF()

    protected val creationSubscriptions = CompositeDisposable()

    private val signedInUserSubject = BehaviorSubject.create<String>()

    private var backgroundSignInCheckThread: Thread? = null

    private val broadcastReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == OTApp.BROADCAST_ACTION_NEW_VERSION_DETECTED) {
                    val versionName = intent.getStringExtra(OTApp.INTENT_EXTRA_LATEST_VERSION_NAME)
                    VersionCheckDialogFragment.makeInstance(versionName)
                            .show(supportFragmentManager, "VersionCheck")

                    systemPreferences.edit()
                            .putString(OTVersionCheckService.PREF_LAST_NOTIFIED_VERSION, versionName)
                            .apply()
                }
            }

        }
    }

    val signedInUserObservable: Observable<String>
        get() = signedInUserSubject
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    private fun refreshCredentialsWithFallbackSignIn() {
        if (authManager.isUserSignedIn() && NetworkHelper.isConnectedToInternet()) {
            //println("${LOG_TAG} OMNITRACK Google is signed in.")
            if (NetworkHelper.isConnectedToInternet()) {
                authManager.refreshCredentialWithFallbackSignIn(this, OmniTrackSignInResultsHandler())
            }
        } else {
            goSignInUnlessUserCached()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as OTApp).applicationComponent.inject(this)

        processAuthorization()
        PreferenceManager.setDefaultValues(this, R.xml.global_preferences, false)
    }

    protected open fun processAuthorization() {
        if (checkRefreshingCredential) {
            val thread = Thread(Runnable {
                refreshCredentialsWithFallbackSignIn()
            })
            thread.start()
        } else {
            goSignInUnlessUserCached()
        }
    }

    private fun goSignInUnlessUserCached() {
        println("OMNITRACK Check whether user is cached. Otherwise, go to sign in")
        creationSubscriptions.add(
                authManager.getAuthStateRefreshObservable().firstOrError().subscribe { signInResult ->
                    if (signInResult > OTAuthManager.SignedInLevel.NONE) {
                        signedInUserSubject.onNext(authManager.userId!!)
                        onSignInProcessCompletelyFinished()
                    } else {
                        goSignIn()
                    }
                }
        )
    }

    fun goSignIn() {
        Log.d(LOG_TAG, "Launching Sign-in Activity...")

        val intent = Intent(this, SignInActivity::class.java)
        //val intent = Intent(this, ExperimentSignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SettingsActivity.REQUEST_CODE) {
            println("returned from settings activity: ${resultCode == RESULT_OK}")
            if (data?.getBooleanExtra(SettingsActivity.FLAG_CONFIGURATION_CHANGED, false) == true) {
                println("configuration was changed from settings")
                recreate()
            }
        } else {
            consentManager.handleActivityResult(false, requestCode, resultCode, data)
        }

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
            try {
                registerReceiver(broadcastReceiver, intentFilter)
            } catch(ex: Exception) {
                ex.printStackTrace()
                println("failed to register update check receiver")
            }
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
            val from = if (intent.hasExtra(OTApp.INTENT_EXTRA_FROM)) {
                intent.getStringExtra(OTApp.INTENT_EXTRA_FROM)
            } else null

            val contentObject = Bundle()
            contentObject.putBoolean("isFinishing", isFinishing)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            EventLoggingManager.logSession(this, now - resumedAt, now, from, contentObject)
        }

        if (checkUpdateAvailable) {
            try {
                unregisterReceiver(broadcastReceiver)
            } catch(ex: Exception) {
                ex.printStackTrace()
                println("failed to unregister update check receiver")
            }
        }
    }

    private fun performSignInProcessCompletelyFinished() {
        println("OMNITRACK loading user from the application global instance")

        this.signedInUserSubject.onNext(authManager.userId!!)

        onSignInProcessCompletelyFinished()
    }

    protected open fun onSignInProcessCompletelyFinished() {

    }

    protected open fun onSessionLogContent(contentObject: Bundle) {
    }

    override fun attachBaseContext(newBase: Context) {

        super.attachBaseContext(CalligraphyContextWrapper.wrap(LocaleHelper.wrapContextWithLocale(newBase, LocaleHelper.getLanguageCode(newBase))))
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