package kr.ac.snu.hcil.omnitrack.services

import android.content.SharedPreferences
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import dagger.internal.Factory
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.kotlin.where
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.Default
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 12. 5..
 */
class OTInformationUploadService : JobService() {

    companion object {
        const val INFORMATION_DEVICE = "deviceInfo"
        const val INFORMATION_USERNAME = "userName"
    }

    @Inject
    lateinit var authManager: OTAuthManager

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var syncServerController: ISynchronizationServerSideAPI

    @field:[Inject Default]
    lateinit var preferences: SharedPreferences

    private val subscriptionDict = Hashtable<String, Disposable>()

    override fun onCreate() {
        super.onCreate()
        (application as OTAndroidApp).currentConfiguredContext.configuredAppComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionDict.forEach { it.value?.dispose() }
        subscriptionDict.clear()
    }

    private fun setSubscription(tag: String, disposable: Disposable) {
        subscriptionDict[tag]?.dispose()
        subscriptionDict[tag] = disposable
    }

    override fun onStartJob(job: JobParameters): Boolean {
        val configuredContext = (application as OTAndroidApp).currentConfiguredContext
        val informationType = job.tag
        if (authManager.isUserSignedIn() && subscriptionDict[informationType]?.isDisposed != false) {
            preferences.edit().putBoolean(informationType, true).apply()
            when (informationType) {
                INFORMATION_DEVICE -> {
                    setSubscription(informationType,
                            OTDeviceInfo.makeDeviceInfo(this@OTInformationUploadService, configuredContext.firebaseComponent).flatMap { deviceInfo ->
                                syncServerController
                                        .putDeviceInfo(deviceInfo)
                            }.doFinally {
                                subscriptionDict.remove(informationType)
                            }.subscribe({ deviceInfoResult ->
                                preferences.edit().putBoolean(informationType, false).apply()
                                jobFinished(job, false)
                            }, { err ->
                                jobFinished(job, true)
                            })
                    )
                }
                INFORMATION_USERNAME -> {
                    val uid = authManager.userId
                    if (uid != null) {
                        setSubscription(informationType, Maybe.defer {
                            val realm = realmFactory.get()
                            val userInfo = realm.where<OTUserDAO>().equalTo("uid", uid).findFirst()
                            return@defer if (userInfo != null) {
                                syncServerController.putUserName(userInfo.name, userInfo.nameUpdatedAt).toMaybe()
                            } else Maybe.empty()
                        }.subscribeOn(Schedulers.io()).subscribe({ result ->
                            if (result.success) {
                                realmFactory.get().use { realm ->
                                    val userInfo = realm.where<OTUserDAO>().equalTo("uid", uid).findFirst()
                                    if (userInfo != null && result.success) {
                                        realm.executeTransaction {
                                            userInfo.name = result.finalValue
                                            userInfo.nameSynchronizedAt = result.payloads?.get("updatedAt")?.toLong()
                                            userInfo.nameSynchronizedAt?.let {
                                                userInfo.nameUpdatedAt = it
                                            }
                                        }
                                    }
                                }
                            }
                        }, { err -> err.printStackTrace() }, {}))
                    }
                }
            }
            return true
        } else return false
    }

    override fun onStopJob(job: JobParameters): Boolean {
        subscriptionDict[job.tag]?.dispose()
        subscriptionDict.remove(job.tag)
        return !preferences.getBoolean(job.tag, false)
    }
}