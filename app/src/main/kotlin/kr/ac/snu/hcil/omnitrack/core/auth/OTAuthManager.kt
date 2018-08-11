package kr.ac.snu.hcil.omnitrack.core.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.ashdavies.rx.rxtasks.toCompletable
import io.ashdavies.rx.rxtasks.toSingle
import io.reactivex.Completable
import io.reactivex.Completable.defer
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.configured.ForGeneralAuth
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignUpActivity
import org.jetbrains.anko.runOnUiThread
import rx_activity_result2.RxActivityResult
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2017-02-03.
 */
@Configured
class OTAuthManager @Inject constructor(
        private val context: Context,
        private val configuredContext: ConfiguredContext,
        private val firebaseAuth: FirebaseAuth,
        @ForGeneric private val gson: Gson,
        @Backend private val realmFactory: Factory<Realm>,
        @ForGeneralAuth private val googleSignInOptions: Lazy<GoogleSignInOptions>,
        private val synchronizationServerController: Lazy<ISynchronizationServerSideAPI>) {

    companion object {
        const val LOG_TAG = "OMNITRACK Auth Manager"
    }
    enum class SignedInLevel {
        NONE, CACHED, AUTHORIZED
    }

    enum class AuthError(@StringRes messageResId: Int) {
        NETWORK_NOT_CONNECTED(0), OMNITRACK_SERVER_NOT_RESPOND(0)
    }

    private val mGoogleApiClient: GoogleApiClient

    init {
        Log.d(LOG_TAG, "Initializing Google SDK...")

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions.get())
                .build()
        mGoogleApiClient.connect()
    }

    fun getDeviceLocalKey(): String? {
        val uid = userId
        if (uid != null) {
            return realmFactory.get().use { realm ->
                realm.where(OTUserDAO::class.java).equalTo("uid", uid)
                        .findFirst()?.uid
            }
        } else return null
    }

    val userId: String?
        get() {
            val id = firebaseAuth.currentUser?.uid
            println("user id: $id")
            return id
        }

    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null && isUserInDb(firebaseAuth.currentUser!!.uid)
    }

    private fun isUserInDb(uid: String): Boolean {
        realmFactory.get().use { realm ->
            return realm.where(OTUserDAO::class.java).equalTo("uid", uid)
                    .findFirst() != null
        }
    }

    val currentSignedInLevel: SignedInLevel get() {
        val result = try {
            if (isUserSignedIn()) {
                return SignedInLevel.AUTHORIZED
            } else return SignedInLevel.NONE
        } catch(ex: Exception) {
            SignedInLevel.NONE
        }

        println("Current signed in level: ${result}")
        return result
    }

    fun loadGoogleSignInAccount(): Maybe<GoogleSignInAccount> {
        return Maybe.defer {
            val connectionResult = mGoogleApiClient.blockingConnect()
            if (connectionResult.isSuccess) {
                return@defer Maybe.just(Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient).await().signInAccount!!)
            } else return@defer Maybe.never<GoogleSignInAccount>()
        }.observeOn(Schedulers.io())
    }

    fun getAuthToken(): Maybe<String> {
        return Maybe.create { disposable ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val task = user.getIdToken(true).addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        val token = result.result.token!!
                        if (!disposable.isDisposed) {
                            disposable.onSuccess(token)
                        }
                    } else {
                        if (!disposable.isDisposed) {
                            disposable.onError(result.exception ?: Exception("Token error"))
                        }
                    }
                }
            } else {
                if (!disposable.isDisposed) {
                    disposable.onComplete()
                }
            }
        }
    }

    private fun signInSilently(): Completable {
        return loadGoogleSignInAccount().flatMapSingle { acc ->
            firebaseAuthWithGoogle(acc)
        }.ignoreElement()
    }

    fun refreshCredentialSilently(skipValidationIfCacheHit: Boolean): Completable {
        return defer {
            if (isUserSignedIn()) {
                if (skipValidationIfCacheHit) {
                    Log.d(LOG_TAG, "Skip sign in. use cached Firebase User.")
                    return@defer Completable.complete()
                } else {
                    Log.d(LOG_TAG, "Reload Firebase User to check connection.")

                    return@defer firebaseAuth.currentUser!!.reload().toCompletable().onErrorResumeNext {
                        signInSilently()
                    }
                }
            } else {
                Log.d(LOG_TAG, "Firebase user does not exist. Sign in silently")
                return@defer signInSilently()
            }
        }

    }

    fun refreshCredentialWithFallbackSignIn(activity: AppCompatActivity): Single<Boolean> {
        return refreshCredentialSilently(false).toSingle { true }.onErrorResumeNext {
            return@onErrorResumeNext startSignInProcess(activity)
        }
    }

    fun getAuthStateRefreshObservable(): Observable<SignedInLevel> {
        return Observable.create { subscriber ->
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val signedInLevel = currentSignedInLevel
                if (!subscriber.isDisposed) {
                    subscriber.onNext(signedInLevel)
                }
            }
            firebaseAuth.addAuthStateListener(listener)

            subscriber.setDisposable(Disposables.fromAction { firebaseAuth.removeAuthStateListener(listener) })
        }
    }

    fun startSignInProcess(activity: AppCompatActivity): Single<Boolean> {
        println("OMNITRACK start sign in activity")
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        return RxActivityResult.on(activity).startIntent(signInIntent).singleOrError().doOnError { clearUserInfo() }.doOnSuccess { if (it.resultCode() == 0) clearUserInfo() }.flatMap { activityResult ->

                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(activityResult.data())
                if (result.isSuccess) {
                    //Signed in successfully.
                    println("signed in google account: ${result.signInAccount}")
                    firebaseAuthWithGoogle(result.signInAccount!!)

                            .flatMap { authResult ->
                                println("Signed in through Google account. try to push device info to server...")
                                OTDeviceInfo.makeDeviceInfo(configuredContext.applicationContext, configuredContext.firebaseComponent)
                            }
                            .flatMap { deviceInfo ->
                                if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
                                    synchronizationServerController.get()
                                            .checkExperimentParticipationStatus(BuildConfig.DEFAULT_EXPERIMENT_ID)
                                            .flatMap { isInExperiment ->
                                                if (!isInExperiment) {
                                                    synchronizationServerController.get().getExperimentConsentInfo(BuildConfig.DEFAULT_EXPERIMENT_ID)
                                                            .flatMap {
                                                                if (BuildConfig.DEFAULT_INVITATION_CODE != null && it.consent == null && it.demographicFormSchema == null) {
                                                                    Single.just(Pair<String, JsonObject?>(BuildConfig.DEFAULT_INVITATION_CODE, null))
                                                                } else RxActivityResult.on(activity)
                                                                        .startIntent(ExperimentSignUpActivity.makeIntent(activity, BuildConfig.DEFAULT_INVITATION_CODE == null, it.consent, it.demographicFormSchema))
                                                                        .firstOrError()
                                                                        .map { result ->
                                                                            if (result.resultCode() != Activity.RESULT_OK) {
                                                                                throw Exception("Canceled the sign up process.")
                                                                            } else {
                                                                                Pair<String, JsonObject?>(
                                                                                        BuildConfig.DEFAULT_INVITATION_CODE
                                                                                                ?: result.data().getStringExtra(ExperimentSignUpActivity.INVITATION_CODE),
                                                                                        result.data().getStringExtra(ExperimentSignUpActivity.DEMOGRAPHIC_SCHEMA)?.let { serializedSchema ->
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
                                    activity.applicationContext.runOnUiThread { notifySignedIn() }
                                    configuredContext.triggerSystemComponent.getTriggerSystemManager().get().checkInAllToSystem(userId!!)
                                }
                            }.doOnError { ex ->
                                ex.printStackTrace()
                                firebaseAuth.signOut()
                                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                            }
                } else {
                    println("Google sign in failed")
                    return@flatMap Single.error<Boolean>(Exception("Google login process was failed."))
                }
        }
    }


    fun handleAuthenticationResult(result: ISynchronizationServerSideAPI.AuthenticationResult): Single<Boolean> {
        return Single.defer {
            val userInfo = result.userInfo
            if (userInfo != null) {
                realmFactory.get().use { realm ->
                    realm.executeTransaction {
                        val user = realm.where(OTUserDAO::class.java).equalTo("uid", userInfo._id).findFirst()
                                ?: realm.createObject(OTUserDAO::class.java, userInfo._id)
                        user.thisDeviceLocalKey = result.deviceLocalKey
                        user.email = userInfo.email
                        user.name = userInfo.name ?: ""
                        user.photoServerPath = userInfo.picture ?: ""
                        user.nameUpdatedAt = userInfo.nameUpdatedAt ?: System.currentTimeMillis()
                        user.nameSynchronizedAt = user.nameUpdatedAt
                    }
                }
                return@defer Single.just(true)
            } else return@defer Single.just(false)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount): Single<AuthResult> {
        return Single.defer {
            Log.d(LOG_TAG, "firebaseAuthWithGooogle:" + acct.id)
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            return@defer firebaseAuth.signInWithCredential(credential).toSingle()
        }
    }

    /*
    fun deleteUser() {
        firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
            task ->
            if (task.isSuccessful) {
                resultsHandler.onSuccess()
                signOut()
            } else {
                resultsHandler.onError(task.exception!!)
            }
        } ?: resultsHandler.onError(Exception("Not signed in."))
    }*/

    fun signOut() {
        val lastUserId = userId
        if (lastUserId != null) {
            clearUserInfo()
            firebaseAuth.signOut()
            Auth.GoogleSignInApi.signOut(mGoogleApiClient)

            configuredContext.triggerSystemComponent.getTriggerSystemManager().get().checkOutAllFromSystem(lastUserId)
            configuredContext.configuredAppComponent.shortcutPanelManager().disposeShortcutPanel()

            notifySignedOut()
        }
    }

    private fun notifySignedIn() {
        context.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_IN)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    private fun notifySignedOut() {
        context.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_OUT)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    fun clearUserInfo() {
        if (userId != null) {
            realmFactory.get().use { realm ->
                realm.executeTransaction { realm ->
                    realm.where(OTUserDAO::class.java).equalTo("uid", userId).findAll().deleteAllFromRealm()
                    /*
                    realm.where(OTTrackerDAO::class.java).equalTo("userId", userId).findAll().deleteAllFromRealm()
                    realm.where(OTTriggerDAO::class.java).equalTo("userId", userId).findAll().deleteAllFromRealm()
                    realm.where(OTItemDAO::class.java).equalTo("userId", userId).findAll().deleteAllFromRealm()*/

                }
            }
        }
    }
}