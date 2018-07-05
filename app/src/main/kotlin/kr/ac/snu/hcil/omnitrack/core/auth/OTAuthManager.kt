package kr.ac.snu.hcil.omnitrack.core.auth

import android.content.Context
import android.content.Intent
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.configured.ForGeneralAuth
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
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


    fun getIsConsentApproved(): Boolean {
        val uid = userId
        if (uid != null) {
            return realmFactory.get().use { realm ->
                realm.where(OTUserDAO::class.java).equalTo("uid", uid)
                        .findFirst()?.consentApproved ?: false
            }
        } else return false
    }

    fun setIsConsentApproved(approved: Boolean) {
        val uid = userId
        if (uid != null) {
            return realmFactory.get().use { realm ->
                realm.where(OTUserDAO::class.java).equalTo("uid", uid)
                        .findFirst()?.let { user ->
                    realm.executeTransaction {
                        user.consentApproved = approved
                    }
                }
            }
        }
    }

    /*
    val userIdObservable = Observable.create<String> {
        subscriber ->
        if (mFirebaseUser != null) {
            mFirebaseUser!!.reload().addOnCompleteListener {
                task ->
                if (task.isSuccessful) {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(mFirebaseUser!!.uid)
                        subscriber.onCompleted()
                    }
                } else {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(Exception("Firebase refresh failed."))
                    }
                }
            }
        } else {
            if (!subscriber.isUnsubscribed) {
                subscriber.onError(Exception("User is not signed in."))
            }
        }
    }.share().observeOn(Schedulers.io())
    */
    val userId: String?
        get() {
            val id = firebaseAuth.currentUser?.uid
            println("user id: $id")
            return id
        }

    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    val currentSignedInLevel: SignedInLevel get() {
        val result = try {
            if (isUserSignedIn()) {
                SignedInLevel.AUTHORIZED
            } else return SignedInLevel.NONE
        } catch(ex: Exception) {
            SignedInLevel.NONE
        }

        println("Current signed in leve: ${result}")
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
        return Completable.defer {
            if (isUserSignedIn()) {
                if (skipValidationIfCacheHit) {
                    Log.d(LOG_TAG, "Skip sign in. use cached Firebase User.")
                    return@defer Completable.complete()
                } else {
                    Log.d(LOG_TAG, "Reload Firebase User to check connection.")
                    return@defer Single.create<Task<Void>> {
                        firebaseAuth.currentUser!!.reload().addOnCompleteListener { task ->
                            if (!it.isDisposed) {
                                it.onSuccess(task)
                            }
                        }
                    }.flatMapCompletable { task ->
                        if (task.isSuccessful) {
                            return@flatMapCompletable Completable.complete()
                        } else signInSilently()
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
                                OTDeviceInfo.makeDeviceInfo(configuredContext.firebaseComponent)
                            }
                            .flatMap { deviceInfo -> synchronizationServerController.get().putDeviceInfo(deviceInfo) }
                            .flatMap { deviceInfoResult ->
                                handlePutDeviceInfoResult(deviceInfoResult).doOnError { ex ->
                                    println("Failed to push device information to server.")
                                    ex.printStackTrace()
                                }
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

    fun handlePutDeviceInfoResult(result: ISynchronizationServerSideAPI.DeviceInfoResult): Single<Boolean> {
        return Single.defer {
            val localKey = result.deviceLocalKey
            if (localKey != null) {
                userId?.let { uid ->
                    realmFactory.get().use { realm ->
                        realm.executeTransaction {
                            val user = realm.where(OTUserDAO::class.java).equalTo("uid", uid).findFirst() ?: realm.createObject(OTUserDAO::class.java, uid)
                            user.thisDeviceLocalKey = localKey

                            if (result.payloads != null) {
                                user.email = result.payloads["email"] ?: ""
                                user.photoServerPath = result.payloads["picture"] ?: ""
                                user.name = result.payloads["name"] ?: ""
                                user.consentApproved = result.payloads["consentApproved"]?.toBoolean() ?: false
                                user.nameUpdatedAt = result.payloads["nameUpdatedAt"]?.toLong() ?: System.currentTimeMillis()
                                user.nameSynchronizedAt = user.nameUpdatedAt
                            }
                        }
                    }

                    return@defer Single.just(true)
                }
            } else return@defer Single.just(false)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount): Single<AuthResult> {
        return Single.create<AuthResult> { subscriber ->
            Log.d(LOG_TAG, "firebaseAuthWithGooogle:" + acct.id)
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { task: Task<AuthResult> ->
                        Log.d(LOG_TAG, "signInWithCredential:onComplete:" + task.isSuccessful)
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Log.w(LOG_TAG, "signInWithCredential", task.exception)
                            if (!subscriber.isDisposed) {
                                subscriber.onError(task.exception!!)
                            }
                        } else {
                            if (!subscriber.isDisposed) {
                                subscriber.onSuccess(task.result)
                            }
                        }
                    }
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

            notifySignedOut()
        }
    }

    private fun notifySignedIn() {
        OTApp.instance.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_IN)
                .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configuredContext.configuration.id)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    private fun notifySignedOut() {
        OTApp.instance.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_OUT)
                .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configuredContext.configuration.id)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    fun clearUserInfo() {
        if (userId != null) {
            realmFactory.get().use { realm ->
                realm.executeTransaction { realm ->
                    realm.where(OTUserDAO::class.java).equalTo("uid", userId).findAll().deleteAllFromRealm()
                }
            }
        }
    }
}