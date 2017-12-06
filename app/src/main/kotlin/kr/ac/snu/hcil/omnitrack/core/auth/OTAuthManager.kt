package kr.ac.snu.hcil.omnitrack.core.auth

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2017-02-03.
 */
class OTAuthManager(val app: OTApp,
                    private val sharedPreferences: SharedPreferences,
                    private val synchronizationServerController: Lazy<ISynchronizationServerSideAPI>) {

    companion object {
        const val LOG_TAG = "OMNITRACK Auth Manager"
        private const val RC_SIGN_IN = 9913
        private val REQUEST_GOOGLE_PLAY_SERVICES = 1363
    }
    enum class SignedInLevel {
        NONE, CACHED, AUTHORIZED
    }

    interface SignInResultsHandler {
        fun onSuccess()
        fun onError(e: Throwable)
        fun onCancel()
    }

    private val mFirebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val mGoogleApiClient: GoogleApiClient

    private val mGoogleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(app.getString(R.string.default_web_client_id))
                .build()
    }

    //is signIn process in progress
    private var mIntentInProgress = false
    private var mResultsHandler: SignInResultsHandler? = null

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    init {
        app.applicationComponent.inject(this)

        Log.d(LOG_TAG, "Initializing Google SDK...")

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = GoogleApiClient.Builder(app)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
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
            val id = mFirebaseAuth.currentUser?.uid
            println("user id: $id")
            return id
        }

    fun isUserSignedIn(): Boolean {
        return mFirebaseAuth.currentUser != null
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
            val user = mFirebaseAuth.currentUser
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

    private fun signInSilently(resultsHandler: SignInResultsHandler) {
        loadGoogleSignInAccount().flatMapObservable { acc ->
            firebaseAuthWithGoogle(acc)
        }.subscribe({ authResult ->
            notifySignedIn(authResult.user)
            resultsHandler.onSuccess()
        }, { ex ->
            resultsHandler.onError(ex)
        })
    }

    fun refreshCredentialSilently(skipValidationIfCacheHit: Boolean, resultsHandler: SignInResultsHandler) {
        if (isUserSignedIn()) {
            if (skipValidationIfCacheHit) {
                Log.d(LOG_TAG, "Skip sign in. use cached Firebase User.")
                resultsHandler.onSuccess()
            } else {
                Log.d(LOG_TAG, "Reload Firebase User to check connection.")
                mFirebaseAuth.currentUser!!.reload().addOnCompleteListener {
                    task ->
                    if (task.isSuccessful) {
                        resultsHandler.onSuccess()
                    } else {
                        signInSilently(resultsHandler)
                    }
                }
            }
        } else {
            Log.d(LOG_TAG, "Firebase user does not exist. Sign in silently")
            signInSilently(resultsHandler)
        }
    }

    fun refreshCredentialWithFallbackSignIn(activity: AppCompatActivity, resultsHandler: SignInResultsHandler) {

        refreshCredentialSilently(false, object : SignInResultsHandler {
            override fun onSuccess() {
                resultsHandler.onSuccess()
            }

            override fun onError(e: Throwable) {
                println("OMNITRACK FallbackSignIn Error")
                startSignInProcess(activity, resultsHandler)
            }

            override fun onCancel() {
                resultsHandler.onCancel()
            }

        })
    }

    fun getAuthStateRefreshObservable(): Observable<SignedInLevel> {
        return Observable.create { subscriber ->
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val signedInLevel = currentSignedInLevel
                if (!subscriber.isDisposed) {
                    subscriber.onNext(signedInLevel)
                }
            }
            mFirebaseAuth.addAuthStateListener(listener)

            subscriber.setDisposable(Disposables.fromAction { mFirebaseAuth.removeAuthStateListener(listener) })
        }
    }

    fun startSignInProcess(activity: AppCompatActivity, resultsHandler: SignInResultsHandler) {
        println("OMNITRACK start sign in activity")
        mResultsHandler = resultsHandler
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        mIntentInProgress = true
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
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
                                user.informationSynchronizedAt = System.currentTimeMillis()
                            }
                        }
                    }

                    return@defer Single.just(true)
                }
            } else return@defer Single.just(false)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            mIntentInProgress = false
            val resultHandler = mResultsHandler
            mResultsHandler = null

            // if the user canceled
            if (resultCode == 0) {
                resultHandler?.onCancel()
                clearUserInfo()
                return
            }

            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                //Signed in successfully.
                println("signed in google account: ${result.signInAccount}")
                firebaseAuthWithGoogle(result.signInAccount!!).flatMap { authResult ->
                    synchronizationServerController.get().putDeviceInfo(OTDeviceInfo()).
                            flatMap { result ->
                                handlePutDeviceInfoResult(result)
                            }
                            .toObservable()
                            .map { success ->
                                println("refreshed device info.")
                                if (success) {
                                    authResult
                                } else throw Exception("Failed to push device info to server.")
                            }
                }.observeOn(AndroidSchedulers.mainThread()).subscribe({ authResult ->
                    println("sign in succeeded.")
                    notifySignedIn(authResult.user)
                    resultHandler?.onSuccess()
                }, {
                    ex ->
                    mFirebaseAuth.signOut()
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                    resultHandler?.onError(ex)
                })

            } else {
                resultHandler!!.onError(Exception("failed"))
                clearUserInfo()
                return
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount): Observable<AuthResult> {
        return Observable.create<AuthResult> { subscriber ->
            Log.d(LOG_TAG, "firebaseAuthWithGooogle:" + acct.id)
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            mFirebaseAuth.signInWithCredential(credential)
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
                                subscriber.onNext(task.result)
                                subscriber.onComplete()
                            }
                        }
                    }
        }
    }

    fun deleteUser(resultsHandler: SignInResultsHandler) {
        mFirebaseAuth.currentUser?.delete()?.addOnCompleteListener {
            task ->
            if (task.isSuccessful) {
                resultsHandler.onSuccess()
                signOut()
            } else {
                resultsHandler.onError(task.exception!!)
            }
        } ?: resultsHandler.onError(Exception("Not signed in."))
    }

    fun signOut() {
        clearUserInfo()
        mFirebaseAuth.signOut()
        Auth.GoogleSignInApi.signOut(mGoogleApiClient)
        notifySignedOut()
    }

    private fun notifySignedIn(user: FirebaseUser) {
        OTApp.instance.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_IN).putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    private fun notifySignedOut() {
        OTApp.instance.sendBroadcast(Intent(OTApp.BROADCAST_ACTION_USER_SIGNED_OUT).putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId))
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

    fun initializeSignInButton(buttonView: View, resultsHandler: SignInResultsHandler): View.OnClickListener? {
        val api = GoogleApiAvailability.getInstance()
        val code = api.isGooglePlayServicesAvailable(buttonView.context.applicationContext)

        val activity = buttonView.getActivity()
        if (activity != null) {
            if (ConnectionResult.SUCCESS != code) {
                if (api.isUserResolvableError(code)) {
                    Log.w(LOG_TAG, "Google Play services recoverable error.")
                    api.showErrorDialogFragment(activity, code, REQUEST_GOOGLE_PLAY_SERVICES)
                } else {
                    val isDebugBuild = 0 != activity
                            .applicationContext
                            .applicationInfo
                            .flags and ApplicationInfo.FLAG_DEBUGGABLE

                    if (!isDebugBuild) {
                        buttonView.visibility = View.GONE
                    } else {
                        Log.w(LOG_TAG, "Google Play Services are not available, but we are showing the Google Sign-in Button, anyway, because this is a debug build.")
                    }
                }
                return null
            }

            val listener = View.OnClickListener { startSignInProcess(activity, resultsHandler) }
            buttonView.setOnClickListener(listener)
            return listener
        } else return null
    }
}