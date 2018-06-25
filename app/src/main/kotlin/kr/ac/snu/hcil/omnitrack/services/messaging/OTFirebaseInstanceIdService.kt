package kr.ac.snu.hcil.omnitrack.services.messaging

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.google.firebase.iid.FirebaseInstanceIdService
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.core.database.global.OTAttachedConfigurationDao
import kr.ac.snu.hcil.omnitrack.core.di.global.AppLevelDatabase
import kr.ac.snu.hcil.omnitrack.services.OTInformationUploadService
import kr.ac.snu.hcil.omnitrack.services.OTInformationUploadService.Companion.INFORMATION_DEVICE
import java.io.IOException
import javax.inject.Inject

/**
 * Receives Firebase Instance Id change event.
 * Created by Young-Ho Kim on 2017. 4. 12.
 */
class OTFirebaseInstanceIdService : FirebaseInstanceIdService() {
    /*
    @Inject
    lateinit var authManager: OTAuthManager
    @Inject
    lateinit var synchronizationServerController: ISynchronizationServerSideAPI

    @Inject
    lateinit var systemPreferences: SharedPreferences
    */

    @Inject
    lateinit var dispatcher: FirebaseJobDispatcher

    @Inject
    lateinit var configController: OTConfigurationController

    @field:[Inject AppLevelDatabase]
    lateinit var globalRealmFactory: Factory<Realm>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        println("Firebase Instance Id Token was refreshed: onTokenRefresh")
        subscriptions.add(
                Completable.merge(
                        configController.map { config ->
                            Completable.defer {
                                val configuredContext = configController.getConfiguredContextOf(config)
                                if (configuredContext != null) {
                                    try {
                                        val fbInstanceId = configuredContext.firebaseComponent.getFirebaseInstanceId()
                                        val token = fbInstanceId.getToken(config.firebaseCloudMessagingSenderId, "FCM")
                                        if (token != null) {
                                            println("FirebaseInstanceId token for ${config.id}: ${token}")
                                            globalRealmFactory.get().use { realm ->
                                                val dao = realm.where(OTAttachedConfigurationDao::class.java)
                                                        .equalTo(OTAttachedConfigurationDao.FIELD_ID, config.id)
                                                        .findFirst()
                                                if (dao != null) {
                                                    realm.executeTransaction {
                                                        dao.firebaseInstanceId = token
                                                        dao.firebaseInstanceIdCreatedAt = fbInstanceId.creationTime
                                                    }
                                                }
                                            }

                                            val currentUserId = configuredContext.configuredAppComponent.getAuthManager().userId
                                            if (currentUserId != null) {
                                                val jobBuilder = configuredContext.scheduledJobComponent.getInformationUploadJobBuilderProvider().get()
                                                dispatcher.mustSchedule(jobBuilder.setTag(OTInformationUploadService.makeTag(
                                                        currentUserId,
                                                        config.id,
                                                        INFORMATION_DEVICE)).build())
                                            }
                                        }
                                    } catch (ex: IOException) {
                                        ex.printStackTrace()
                                    }
                                }
                                return@defer Completable.complete()
                            }
                        }
                ).subscribeOn(Schedulers.io())
                        .subscribe({
                            println("Firebase Instance Id Token was refreshed: onTokenRefresh")
                        }, { ex ->
                            ex.printStackTrace()
                        })
        )
    }

    /*
    private fun refreshInstanceIdToServerIfExists(): Single<Boolean> {

        val token = FirebaseInstanceId.getInstance().token
        if (token != null && authManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
            systemPreferences.edit().putString(OTApp.PREFERENCE_KEY_FIREBASE_INSTANCE_ID, token)
                    .apply()
            return synchronizationServerController.postDeviceInfo(OTDeviceInfo()).flatMap { deviceInfoResult ->
                authManager.handlePutDeviceInfoResult(deviceInfoResult)
            }
        } else {
            return Single.just(false)
        }
    }*/
}