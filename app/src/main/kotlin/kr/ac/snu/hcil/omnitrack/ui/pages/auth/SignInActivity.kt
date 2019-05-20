package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_sign_in.*
import kr.ac.snu.hcil.android.common.net.NetworkNotConnectedException
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.android.common.view.text.LowercaseInputFilter
import kr.ac.snu.hcil.android.common.view.text.NoBlankInputFilter
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.di.global.ServerResponsive
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.ServerError
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Provider

class SignInActivity : AppCompatActivity(R.layout.activity_sign_in), TextWatcher {

    companion object {
        private val LOG_TAG = "SignInActivity"
    }

    @Inject
    protected lateinit var authManager: OTAuthManager

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    @field:[Inject ForGeneric]
    protected lateinit var gson: Gson

    @field:[Inject ServerResponsive]
    protected lateinit var checkServerConnection: Provider<Completable>

    @Inject
    protected lateinit var serverApi: ISynchronizationServerSideAPI

    private val creationSubscription = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as OTAndroidApp).applicationComponent.inject(this)

        ui_app_title.text = BuildConfig.APP_NAME
        toIdleMode()

        ui_button_signup.setOnClickListener {
            startActivity(SignUpActivity.makeIntent(this))
        }

        ui_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        ui_textfield_signin_username.filters += arrayOf(LowercaseInputFilter(), NoBlankInputFilter())

        ui_textfield_signin_username.addTextChangedListener(this)
        ui_textfield_signin_password.addTextChangedListener(this)

        ui_button_signin.setOnClickListener {
            trySignIn()
        }

        ui_textfield_signin_password.setOnEditorActionListener { v, actionId, event ->
            trySignIn()
            false
        }
    }

    private fun trySignIn() {
        val username = ui_textfield_signin_username.text?.toString() ?: ""
        val password = ui_textfield_signin_password.text?.toString() ?: ""

        val usernameInvalidMessage = authManager.validateUsername(username)
        val passwordInvalidMessage = authManager.validatePassword(password)

        ui_form_signin_username.error = usernameInvalidMessage
        ui_form_signin_password.error = passwordInvalidMessage

        if (usernameInvalidMessage == null && passwordInvalidMessage == null) {
            //valid
            toBusyMode()
            creationSubscription.add(
                    checkServerConnection.get()
                            .andThen(serverApi.validateClientCertified())
                            .andThen(authManager.authenticate(username, password))
                            .observeOn(AndroidSchedulers.mainThread()).doAfterTerminate {
                                toIdleMode()
                            }.subscribe(
                                    {
                                        ui_form_signin_username.error = null
                                        ui_form_signin_password.error = null
                                        Toast.makeText(this@SignInActivity, String.format(getString(R.string.msg_signin_succeeded)), Toast.LENGTH_LONG).show()
                                        goHomeActivity()
                                    },
                                    { e ->
                                        e.printStackTrace()
                                        if (e is NetworkNotConnectedException) {
                                            val noticeDialogBuilder = AlertDialog.Builder(this@SignInActivity)
                                            noticeDialogBuilder
                                                    .setTitle(BuildConfig.APP_NAME)
                                                    .setMessage("The server does not respond. Check your internet connection.")
                                                    .setNeutralButton("Ok", null)
                                                    .show()
                                        } else if (e is HttpException) {
                                            val serverErrorCode = ServerError.extractServerErrorCode(gson, e)
                                            when (serverErrorCode) {
                                                ServerError.ERROR_CODE_WRONG_CREDENTIAL -> {
                                                    ui_form_signin_username.error = getString(R.string.auth_signin_wrong_credential)
                                                    ui_form_signin_password.error = getString(R.string.auth_signin_wrong_credential)
                                                }
                                                ServerError.ERROR_CODE_UNCERTIFIED_CLIENT -> {
                                                    val noticeDialogBuilder = DialogHelper.makeSimpleAlertBuilder(
                                                            this,
                                                            "This application is not certified in the server. You can't use this app unless the admin researcher registers this app.",
                                                            "Client Not Verified")
                                                    noticeDialogBuilder.show()
                                                }
                                            }
                                        } else {
                                            val errorDialogBuilder = DialogHelper.makeSimpleAlertBuilder(this@SignInActivity, String.format("Sign-in Process failed.\n%s", e.message), "Sign-In Error", R.string.msg_ok)
                                            errorDialogBuilder.show()
                                            eventLogger.get().logExceptionEvent("SignInFailure", e, Thread.currentThread())
                                        }
                                    }
                            )
            )
        }
    }

    override fun afterTextChanged(s: Editable?) {
        ui_button_signin.isEnabled = ui_textfield_signin_password.text?.isNotBlank() == true && ui_textfield_signin_username.text?.isNotBlank() == true
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    private fun toBusyMode() {
        runOnUiThread {
            ui_group_signin.visibility = View.GONE
            ui_loading_indicator.visibility = View.VISIBLE
        }
    }

    private fun toIdleMode() {
        runOnUiThread {
            ui_group_signin.visibility = View.VISIBLE
            ui_loading_indicator.visibility = View.GONE
        }
    }

    private fun goHomeActivity() {
        eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_SIGNED_IN)
        Log.d(LOG_TAG, "Launching Main Activity...")
        finishAffinity()
        startActivity(Intent(this@SignInActivity, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(OTApp.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true)
                .putExtra(HomeActivity.INTENT_EXTRA_INITIAL_LOGIN, true)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        creationSubscription.clear()
    }
}