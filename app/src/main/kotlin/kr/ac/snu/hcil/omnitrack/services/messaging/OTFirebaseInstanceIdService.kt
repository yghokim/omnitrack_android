package kr.ac.snu.hcil.omnitrack.services.messaging

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 12..
 */
class OTFirebaseInstanceIdService : FirebaseInstanceIdService() {
    @Inject
    lateinit var authManager: OTAuthManager
    @Inject
    lateinit var synchronizationServerController: ISynchronizationServerSideAPI

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        println("Firebase cloud messaging instance id refreshed. token: ${FirebaseInstanceId.getInstance().token}")

        subscriptions.add(
            refreshInstanceIdToServerIfExists(false).subscribe {
                success->
                println("revised firebase instance id was successfully sent to server.")
            }
        )
    }

    fun refreshInstanceIdToServerIfExists(ignoreIfStored: Boolean): Single<Boolean> {
        if (ignoreIfStored) {
            if (OTApp.instance.systemSharedPreferences.contains(OTApp.PREFERENCE_KEY_FIREBASE_INSTANCE_ID)) {
                return Single.just(false)
            }
        }

        val token = FirebaseInstanceId.getInstance().token
        if (token != null && authManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
            OTApp.instance.systemSharedPreferences.edit().putString(OTApp.PREFERENCE_KEY_FIREBASE_INSTANCE_ID, token)
                    .apply()
            return synchronizationServerController.putDeviceInfo(OTDeviceInfo()).map { deviceInfoResult ->
                val localKey = deviceInfoResult.deviceLocalKey
                if (localKey != null) {
                    authManager.userDeviceLocalKey = localKey
                    return@map true
                } else return@map false
            }
        } else {
            return Single.just(false)
        }
    }
}