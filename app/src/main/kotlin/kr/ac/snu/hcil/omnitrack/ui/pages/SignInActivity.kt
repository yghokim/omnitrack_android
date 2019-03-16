package kr.ac.snu.hcil.omnitrack.ui.pages

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.GoogleApiAvailability
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_sign_in.*
import kr.ac.snu.hcil.android.common.net.NetworkNotConnectedException
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.di.global.ServerResponsive
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import rx_activity_result2.Result
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Provider

class SignInActivity : AppCompatActivity() {

    @Inject
    protected lateinit var authManager: OTAuthManager

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    @field:[Inject ServerResponsive]
    protected lateinit var checkServerConnection: Provider<Completable>

    private val creationSubscription = CompositeDisposable()

    private fun startSignIn() {
        startActivityForResult(authManager.makeSignIntent(), OTAuthManager.GOOGLE_SIGN_IN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OTAuthManager.GOOGLE_SIGN_IN_REQUEST_CODE) {
            creationSubscription.add(
                    authManager.handleSignInProcessResult(Result<AppCompatActivity>(this, requestCode, resultCode, data))
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
                            .subscribe({ approved ->
                                if (approved) {
                                    goHomeActivity()
                                } else {
                                    toIdleMode()
                                }
                            }, { e ->
                                e.printStackTrace()
                                if (e is CancellationException) {
                                    Log.d(LOG_TAG, String.format("User sign-in with Google canceled."))
                                    this@SignInActivity.runOnUiThread {
                                        Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_canceled)), Toast.LENGTH_LONG).show()
                                    }

                                } else {
                                    val errorDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                    errorDialogBuilder.setTitle("Sign-In Error")
                                    errorDialogBuilder.setMessage(
                                            String.format("Sign-in Process failed.\n%s", e.message))
                                    errorDialogBuilder.setNeutralButton("Ok", null)
                                    errorDialogBuilder.show()
                                    eventLogger.get().logExceptionEvent("SignInFailure", e, Thread.currentThread())
                                }
                                toIdleMode()
                            }
                            )
            )
        }
    }

    /*
    creationSubscription.add(
                        checkServerConnection.get().andThen(authManager.startSignInProcess(this))
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
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ approved ->
                                    if (approved) {
                                        goHomeActivity()
                                    } else {
                                        toIdleMode()
                                    }
                                }, { e ->
                                    e.printStackTrace()
                                    if (e is NetworkNotConnectedException) {
                                        val noticeDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                        noticeDialogBuilder
                                                .setTitle(BuildConfig.APP_NAME)
                                                .setMessage("The app server does not respond. Try again later.")
                                                .setNeutralButton("Ok", null)
                                                .show()
                                    } else if (e is CancellationException) {
                                        Log.d(LOG_TAG, String.format("User sign-in with Google canceled."))
                                        this@SignInActivity.runOnUiThread {
                                            Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_sign_in_google_canceled)), Toast.LENGTH_LONG).show()
                                        }

                                    } else {
                                        val errorDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                        errorDialogBuilder.setTitle("Sign-In Error")
                                        errorDialogBuilder.setMessage(
                                                String.format("Sign-in Process failed.\n%s", e.message))
                                        errorDialogBuilder.setNeutralButton("Ok", null)
                                        errorDialogBuilder.show()
                                        eventLogger.get().logExceptionEvent("SignInFailure", e, Thread.currentThread())
                                    }
                                    toIdleMode()
                                }
                                )
                )*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as OTAndroidApp).applicationComponent.inject(this)

        setContentView(R.layout.activity_sign_in)

        ui_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val task = GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        task.addOnCompleteListener { apiCheckTask ->
            ui_login_group_switcher.visibility = View.VISIBLE
            g_login_button.setOnClickListener(View.OnClickListener { view ->
                toBusyMode()
                creationSubscription.add(
                        checkServerConnection.get()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    startSignIn()
                                }, { e ->
                                    e.printStackTrace()
                                    if (e is NetworkNotConnectedException) {
                                        val noticeDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                        noticeDialogBuilder
                                                .setTitle(BuildConfig.APP_NAME)
                                                .setMessage("The app server does not respond. Try again later.")
                                                .setNeutralButton("Ok", null)
                                                .show()
                                    }
                                    toIdleMode()
                                })
                )
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