package kr.ac.snu.hcil.omnitrack.core.auth

import android.content.Intent
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import java.util.concurrent.Executors

/**
 * Created by Young-Ho Kim on 2017-02-03.
 */
object OTAuthManager {

    const val LOG_TAG = "OMNITRACK Auth Manager"

    enum class SignedInLevel {
        NONE, AUTHORIZED, CACHED
    }

    interface SignInResultsHandler {
        fun onSuccess()
        fun onError(e: Throwable)
        fun onCancel()
    }

    private const val RC_SIGN_IN = 9913
    private val REQUEST_GOOGLE_PLAY_SERVICES = 1363


    private val executorService = Executors.newFixedThreadPool(2)

    private val mFirebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val mGoogleApiClient: GoogleApiClient
    private var googleSignInAccount: GoogleSignInAccount? = null

    private val mGoogleSignInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestProfile()
            .requestEmail()
            .requestIdToken(OTApplication.getString(R.string.default_web_client_id))
            .build()

    //is signIn process in progress
    private var mIntentInProgress = false
    private var mResultsHandler: SignInResultsHandler? = null

    @Volatile var authToken: String? = null
        private set

    var email: String? = null
        private set

    var userName: String? = null
        private set

    var userImageUrl: String? = null
        private set

    init {
        clearUserInfo()

        mFirebaseAuth.addIdTokenListener { auth ->
            auth.currentUser?.getIdToken(false)?.addOnCompleteListener(object : OnCompleteListener<GetTokenResult> {
                override fun onComplete(task: Task<GetTokenResult>) {
                    authToken = task.result.token
                }
            })
        }

        Log.d(LOG_TAG, "Initializing Google SDK...")

        // Build GoogleApiClient with access to basic profile
        mGoogleApiClient = GoogleApiClient.Builder(OTApplication.app)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                .build()
        mGoogleApiClient.connect()
    }

    fun makeUserInstance(): OTUser? {
        if (userId != null) {
            return OTUser(userId!!, userName, userImageUrl)
        } else return null
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
    val userId: String? get() = mFirebaseAuth.currentUser?.uid

    var userDeviceLocalKey: String?
        get() = OTApplication.app.systemSharedPreferences.getString(OTApplication.PREFERENCE_KEY_DEVICE_LOCAL_KEY, null)
        set(value) {
            println("set device local key: ${value}")
            if (value != null) {
                OTApplication.app.systemSharedPreferences.edit().putString(OTApplication.PREFERENCE_KEY_DEVICE_LOCAL_KEY, value).apply()
            } else {
                OTApplication.app.systemSharedPreferences.edit().remove(OTApplication.PREFERENCE_KEY_DEVICE_LOCAL_KEY).apply()
            }
        }

    fun isUserSignedIn(): Boolean {
        return mFirebaseAuth.currentUser != null
    }

    val currentSignedInLevel: SignedInLevel get() {
        val result = try {
            if (isUserSignedIn()) {
                SignedInLevel.AUTHORIZED
            } else if (OTUser.isUserStored(OTApplication.app.systemSharedPreferences)) {
                SignedInLevel.CACHED
            } else return SignedInLevel.NONE
        } catch(ex: Exception) {
            SignedInLevel.NONE
        }

        println("Current signed in leve: ${result}")
        return result
    }

    fun refreshGoogleAccount(finished: (Boolean) -> Unit) {
        Thread {
            val result = mGoogleApiClient.blockingConnect()
            if (result.isSuccess) {
                try {
                    googleSignInAccount = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient).await().signInAccount
                    Log.d("OMNITRACK", "Google SigninAccount: " + googleSignInAccount)
                    refreshToken()
                    finished.invoke(true)
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Failed to update Google token", e)
                    finished.invoke(false)
                }

            } else finished.invoke(false)
        }.start()
    }

    private fun signInSilently(resultsHandler: SignInResultsHandler) {
        refreshGoogleAccount {
            success ->
            if (success) {
                firebaseAuthWithGoogle(googleSignInAccount).subscribe({
                    authResult ->
                    reloadUserInfo()
                    notifySignedIn(authResult.user)
                    resultsHandler.onSuccess()
                }, {
                    ex ->
                    resultsHandler.onError(ex)
                })
            } else {
                resultsHandler.onError(Exception("Google sign in failed."))
            }
        }
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
                googleSignInAccount = result.signInAccount
                println("signed in google account: ${result.signInAccount}")
                refreshToken()
                firebaseAuthWithGoogle(googleSignInAccount).flatMap { authResult ->
                    OTApplication.app.synchronizationServerController.putDeviceInfo(OTDeviceInfo()).
                            map { result ->
                                val localKey = result.deviceLocalKey
                                if (localKey != null) {
                                    userDeviceLocalKey = localKey
                                    return@map true
                                } else return@map false
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
                    reloadUserInfo()
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

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?): Observable<AuthResult> {
        return Observable.create<AuthResult> {
            subscriber ->
            Log.d(LOG_TAG, "firebaseAuthWithGooogle:" + acct?.id)
            val credential = getAuthCredential()
            mFirebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener {
                        task: Task<AuthResult> ->
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
        val uid = userId
        if (uid != null) {
            DatabaseManager.removeDeviceInfo(uid, OTApplication.app.deviceId).subscribe {
                clearUserInfo()
                mFirebaseAuth.signOut()
                Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                userDeviceLocalKey = null
                notifySignedOut()
            }
        } else {
            clearUserInfo()
            mFirebaseAuth.signOut()
            Auth.GoogleSignInApi.signOut(mGoogleApiClient)
            userDeviceLocalKey = null
            notifySignedOut()
        }
    }

    private fun notifySignedIn(user: FirebaseUser) {
        OTApplication.app.sendBroadcast(Intent(OTApplication.BROADCAST_ACTION_USER_SIGNED_IN).putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    private fun notifySignedOut() {
        OTApplication.app.sendBroadcast(Intent(OTApplication.BROADCAST_ACTION_USER_SIGNED_OUT).putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, userId))
    }

    fun clearUserInfo() {
        userName = null
        userImageUrl = null
        email = null

    }

    fun refreshToken(): String? {
        Log.d(LOG_TAG, "Google provider refreshing token...")

        try {
            Log.w(LOG_TAG, "update Google token")
            authToken = this.googleSignInAccount?.idToken
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to update Google token", e)
            authToken = null
        }

        return authToken
    }

    fun getAuthCredential(): AuthCredential {
        return GoogleAuthProvider.getCredential(authToken, null)
    }

    fun reloadUserInfo() {
        userName = googleSignInAccount?.displayName
        email = googleSignInAccount?.email
        Log.v("OMNITRACK", "email: " + email)
        val photoUrl = googleSignInAccount?.photoUrl
        if (photoUrl != null) {

            Log.w(LOG_TAG, "Google photo uri:" + photoUrl.toString())
            userImageUrl = photoUrl.toString()
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