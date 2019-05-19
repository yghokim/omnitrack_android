package kr.ac.snu.hcil.omnitrack.core.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Completable.defer
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.Default
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Young-Ho Kim on 2017-02-03.
 */
@Singleton
class OTAuthManager @Inject constructor(
        private val context: Context,
        @Default private val sharedPreferences: SharedPreferences,
        private val triggerSystemManager: Lazy<OTTriggerSystemManager>,
        private val shortcutPanelManager: OTShortcutPanelManager,
        private val eventLogger: Lazy<IEventLogger>,
        @ForGeneric private val gson: Gson,
        @Backend private val realmFactory: Factory<Realm>,
        private val authApiController: Lazy<OTAuthApiController>,
        private val synchronizationServerController: Lazy<ISynchronizationServerSideAPI>) {

    companion object {
        const val LOG_TAG = "OMNITRACK Auth Manager"
        const val PREF_KEY_TOKEN = "auth_jwt"
        const val PREF_DEVICE_LOCAL_KEY = "device_local_key"

        const val MIN_LENGTH_USERNAME = 3
        const val MAX_LENGTH_USERNAME = 50

        const val MIN_LENGTH_PASSWORD = 4
    }
    enum class SignedInLevel {
        NONE, CACHED, AUTHORIZED
    }

    enum class AuthError(@StringRes messageResId: Int) {
        NETWORK_NOT_CONNECTED(0), OMNITRACK_SERVER_NOT_RESPOND(0)
    }

    private val tokenChangedSubject = PublishSubject.create<String>()
    val authTokenChanged: Subject<String> get() = tokenChangedSubject

    private var decodedTokenCache: JWT? = null
    private val decodedToken: JWT?
        @Synchronized get() {
            if (decodedTokenCache == null) {
                val storedToken = getCurrentAuthToken()
                if (storedToken != null) {
                    decodedTokenCache = JWT(storedToken)
                }
            }
            return decodedTokenCache
        }

    private fun updateToken(newToken: String) {
        decodedTokenCache = null
        sharedPreferences.edit().putString(PREF_KEY_TOKEN, newToken).apply()
        tokenChangedSubject.onNext(newToken)
    }

    val deviceLocalKey: String?
        get() {
            return sharedPreferences.getString(PREF_DEVICE_LOCAL_KEY, null)
        }

    val userName: String?
        get() {
            return decodedToken?.getClaim("username").toString()
        }

    val userId: String?
        get() {
            val id = decodedToken?.getClaim("uid").toString()
            println("user id: $id")
            return id
        }

    fun isUserSignedIn(): Boolean {
        return decodedToken?.isExpired(0) == false
    }

    val currentSignedInLevel: SignedInLevel get() {
        val result = try {
            if (isUserSignedIn()) {
                return SignedInLevel.AUTHORIZED
            } else return SignedInLevel.NONE
        } catch(ex: Exception) {
            SignedInLevel.NONE
        }

        println("Current signed in level: $result")
        return result
    }

    fun getCurrentAuthToken(): String? {
        return sharedPreferences.getString(PREF_KEY_TOKEN, null)
    }

    fun refreshCredentialSilently(skipValidationIfCacheHit: Boolean): Completable {
        return defer {
            if (isUserSignedIn()) {
                if (skipValidationIfCacheHit) {
                    Log.d(LOG_TAG, "Skip sign in. use cached user token.")
                    return@defer Completable.complete()
                }
            }

            val token = getCurrentAuthToken()
            return@defer if (token != null) {
                authApiController.get().refreshToken(token).doAfterSuccess { newToken ->
                    updateToken(newToken)
                }.ignoreElement()
            } else Completable.error(IllegalAccessException())
        }

    }

    fun authenticate(username: String, password: String): Completable {
        return OTDeviceInfo.makeDeviceInfo(context)
                .flatMap { authApiController.get().authenticate(username, password, it) }
                .doOnSuccess { responseData ->
                    handleAuthResult(responseData, true)
                }.ignoreElement()
    }

    fun validateUsername(input: String): String? {
        if (input.isBlank()) {
            return context.resources.getString(R.string.msg_auth_validation_error_username_blank)
        } else if (input.length < MIN_LENGTH_USERNAME || input.length > MAX_LENGTH_USERNAME) {
            return String.format(context.resources.getString(R.string.msg_auth_validation_error_username_length), MIN_LENGTH_USERNAME, MAX_LENGTH_USERNAME)
        } else if (input.matches(Regex.fromLiteral("^[a-z][a-z0-9]+\$")) != false || !input[0].isLetter()) {
            return context.resources.getString(R.string.msg_auth_validation_error_username_alphanumeric)
        } else return null
    }

    /***
     * Null is validated.
     */
    fun validatePassword(input: String): String? {
        if (input.isBlank()) {
            return context.resources.getString(R.string.msg_auth_validation_error_password_blank)
        } else if (input.length < MIN_LENGTH_PASSWORD) {
            return String.format(context.resources.getString(R.string.msg_auth_validation_error_password_length), MIN_LENGTH_PASSWORD)
        } else return null
    }

    fun register(username: String, password: String, invitationCode: String?, demographicData: JsonObject?): Completable {
        return OTDeviceInfo.makeDeviceInfo(context).flatMap { deviceInfo ->
            authApiController.get().register(username, password, deviceInfo, invitationCode, demographicData)
        }.doOnSuccess { responseData ->
            handleAuthResult(responseData, true)
        }.ignoreElement()
    }


    private fun handleAuthResult(responseData: OTAuthApiController.AuthResponseData, firstSignIn: Boolean) {
        updateToken(responseData.token)

        sharedPreferences.edit().putString(PREF_DEVICE_LOCAL_KEY, responseData.deviceLocalKey).apply()

        if (firstSignIn) {
            context.runOnUiThread { notifySignedIn() }
            triggerSystemManager.get().checkInAllToSystem(userId!!)
        }
    }

    /* TODO handle 'sign up' process.

    fun handleSignInProcessResult(activityResult: Result<AppCompatActivity>): Single<Boolean> {
        return Single.defer {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(activityResult.data())
            if (result.isSuccess) {
                //Signed in successfully.
                println("signed in google account: ${result.signInAccount}")
                firebaseAuthWithGoogle(result.signInAccount!!)

                        .flatMap { authResult ->
                            println("Signed in through Google account. try to push device info to server...")
                            OTDeviceInfo.makeDeviceInfo(context)
                        }
                        .flatMap { deviceInfo ->
                            if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
                                synchronizationServerController.get()
                                        .checkExperimentParticipationStatus(BuildConfig.DEFAULT_EXPERIMENT_ID)
                                        .flatMap { isInExperiment ->
                                            if (!isInExperiment) {
                                                synchronizationServerController.get().getExperimentConsentInfo(BuildConfig.DEFAULT_EXPERIMENT_ID)
                                                        .flatMap {
                                                            if (BuildConfig.DEFAULT_INVITATION_CODE != null && (!it.receiveConsentInApp || (it.consent == null && it.demographicFormSchema == null))) {
                                                                Single.just(Pair<String, JsonObject?>(BuildConfig.DEFAULT_INVITATION_CODE, null))
                                                            } else RxActivityResult.on(activityResult.targetUI())
                                                                    .startIntent(SignUpActivity.makeIntent(activityResult.targetUI(), BuildConfig.DEFAULT_INVITATION_CODE == null, if (it.receiveConsentInApp) it.consent else null, if (it.receiveConsentInApp) it.demographicFormSchema else null))
                                                                    .firstOrError()
                                                                    .map { result ->
                                                                        if (result.resultCode() != Activity.RESULT_OK) {
                                                                            throw CancellationException()
                                                                        } else {
                                                                            Pair<String, JsonObject?>(
                                                                                    BuildConfig.DEFAULT_INVITATION_CODE
                                                                                            ?: result.data().getStringExtra(SignUpActivity.INVITATION_CODE),
                                                                                    result.data().getStringExtra(SignUpActivity.DEMOGRAPHIC_SCHEMA)?.let { serializedSchema ->
                                                                                        gson.fromJson(serializedSchema, JsonObject::class.java)
                                                                                    })
                                                                        }
                                                                    }
                                                        }.flatMap {
                                                            synchronizationServerController.get().authenticate(deviceInfo, it.first, it.second)
                                                        }
                                            } else synchronizationServerController.get().authenticate(deviceInfo, null, null)
                                        }
                            } else {
                                //Pure experiment-free authentication
                                synchronizationServerController.get().authenticate(deviceInfo, null, null)
                            }
                        }
                        .flatMap { authenticationResult ->
                            handleAuthenticationResult(authenticationResult)
                        }
                        .doOnSuccess { success ->
                            if (success) {
                                activityResult.targetUI().applicationContext.runOnUiThread { notifySignedIn() }
                                triggerSystemManager.get().checkInAllToSystem(userId!!)
                            }
                        }.doOnError { ex ->
                            ex.printStackTrace()
                            firebaseAuth.signOut()
                            Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                        }
            } else if (result.status.isCanceled || result.status.statusCode == 12501) {
                throw CancellationException(result.status.statusMessage)
            } else {
                println("Google sign in failed")
                eventLogger.get().logExceptionEvent("GoogleSignInError",
                        Exception(result.status.statusMessage),
                        Thread.currentThread()) { json ->
                    json.addProperty("statusCode", result.status.statusCode)
                    json.addProperty("isInterrupted", result.status.isInterrupted)
                    json.addProperty("isCanceled", result.status.isCanceled)
                    json.addProperty("hasResolution", result.status.hasResolution())
                }
                return@defer Single.error<Boolean>(Exception("Google login process was failed. status code: ${result.status.statusCode}, message: ${result.status.statusMessage}"))
            }
        }
    }*/

    fun signOut(): Completable {
        val lastUserId = userId
        if (lastUserId != null) {
            return OTDeviceInfo.makeDeviceInfo(context).flatMapCompletable { authApiController.get().signOut(it) }.doOnComplete {

                decodedTokenCache = null
                sharedPreferences.edit()
                        .remove(PREF_KEY_TOKEN)
                        .remove(PREF_DEVICE_LOCAL_KEY).apply()

                triggerSystemManager.get().checkOutAllFromSystem(lastUserId)
                shortcutPanelManager.disposeShortcutPanel()
                notifySignedOut()
            }
        } else return Completable.complete()
    }

    private fun notifySignedIn() {
        val intent = Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_IN)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId)
        context.sendBroadcast(intent)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun notifySignedOut() {
        val intent = Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_OUT)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId)
        context.sendBroadcast(intent)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}