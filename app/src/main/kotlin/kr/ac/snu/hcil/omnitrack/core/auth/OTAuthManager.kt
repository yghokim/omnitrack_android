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
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.BinaryStorageServer
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.di.global.UserInfo
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.core.serialization.getStringCompat
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.system.OTAppFlagManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.runOnUiThread
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Young-Ho Kim on 2017-02-03.
 */
@Singleton
class OTAuthManager @Inject constructor(
        private val context: Context,
        @UserInfo private val sharedPreferences: SharedPreferences,
        @Backend private val realmFactory: Factory<Realm>,
        @BinaryStorageServer private val binaryUploadTaskRealmFactory: Factory<Realm>,
        @ForGeneric private val gson: Lazy<Gson>,
        private val syncManager: OTSyncManager,
        private val triggerSystemManager: Lazy<OTTriggerSystemManager>,
        private val shortcutPanelManager: OTShortcutPanelManager,
        private val authApiController: Lazy<IAuthServerAPI>,
        private val binaryStorageController: Lazy<OTBinaryStorageController>,
        private val appFlagManager: OTAppFlagManager) {

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
            return decodedToken?.getClaim("username")?.asString()
        }

    val email: String?
        get() {
            return decodedToken?.getClaim("email")?.asString()
        }

    val userId: String?
        get() {
            val id = decodedToken?.getClaim("uid")?.asString()
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

    fun register(username: String, email: String, password: String, invitationCode: String?, demographicData: JsonObject?): Completable {
        return OTDeviceInfo.makeDeviceInfo(context).flatMap { deviceInfo ->
            authApiController.get().register(username, email, password, deviceInfo, invitationCode, demographicData)
        }.doOnSuccess { responseData ->
            handleAuthResult(responseData, true)
        }.ignoreElement()
    }


    private fun handleAuthResult(responseData: IAuthServerAPI.AuthResponseData, firstSignIn: Boolean) {

        sharedPreferences.edit().putString(PREF_DEVICE_LOCAL_KEY, responseData.deviceLocalKey).apply()
        appFlagManager.updateAppFlags(responseData.appFlags)

        updateToken(responseData.token)

        if (firstSignIn) {
            context.runOnUiThread { notifySignedIn() }
            triggerSystemManager.get().checkInAllToSystem(userId!!)
            syncManager.reservePeriodicSyncWorker()
        }
    }

    fun signOut(): Completable {
        val lastUserId = userId
        if (lastUserId != null) {
            return OTDeviceInfo.makeDeviceInfo(context).flatMapCompletable { authApiController.get().signOut(it) }.doOnComplete {

                decodedTokenCache = null
                sharedPreferences.edit().clear().apply()

                triggerSystemManager.get().checkOutAllFromSystem(lastUserId)
                shortcutPanelManager.disposeShortcutPanel()
                syncManager.clearSynchronizationOnDevice()
                binaryStorageController.get().clearWorkersOnDevice()

                realmFactory.get().use { realm ->
                    realm.executeTransactionIfNotIn {
                        realm.deleteAll()
                    }
                }

                binaryUploadTaskRealmFactory.get().use { realm ->
                    realm.executeTransactionIfNotIn {
                        realm.deleteAll()
                    }
                }

                notifySignedOut()
            }
        } else return Completable.complete()
    }

    fun updateEmail(email: String): Completable {
        return authApiController.get().updateEmail(email).doOnSuccess { response ->
            updateToken(response.token)
        }.ignoreElement()
    }

    fun changePassword(original: String, newPassword: String): Completable {
        return authApiController.get().changePassword(original, newPassword).doOnSuccess { response ->
            updateToken(response.token)
        }.ignoreElement().onErrorResumeNext { err ->
            if (err is HttpException) {
                val errorBody = gson.get().fromJson<JsonObject>(
                        err.response().errorBody()?.charStream(), JsonObject::class.java)

                if (errorBody != null) {
                    when (errorBody.getStringCompat("error")) {
                        "CredentialWrong" -> {
                            Completable.error(DialogHelper.ChangePasswordWrongException(originalPasswordErrorMessage = "Inserted wrong password."))
                        }
                        else -> throw err
                    }
                } else throw err
            } else throw err
        }
    }

    fun dropOutFromStudy(reason: String?): Completable {
        return authApiController.get().dropOutFromStudy(reason).andThen { signOut() }
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