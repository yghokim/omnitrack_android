package kr.ac.snu.hcil.omnitrack.services

import android.content.SharedPreferences
import com.firebase.jobdispatcher.JobParameters
import dagger.internal.Factory
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.kotlin.where
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.configured.ConfiguredObject
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 12. 5..
 */
class OTInformationUploadService : ConfigurableJobService() {

    companion object {
        const val TAG_DELIMITER = ';'
        const val INFORMATION_DEVICE = "deviceInfo"
        const val INFORMATION_USERNAME = "userName"

        fun makeTag(userId: String, configurationId: String, informationType: String): String {
            return "$configurationId;$userId;$informationType"
        }

        fun getConfigId(tag: String): String {
            return tag.split(TAG_DELIMITER)[0]
        }

        fun getUserId(tag: String): String {
            return tag.split(TAG_DELIMITER)[1]
        }

        fun getInformationType(tag: String): String {
            return tag.split(TAG_DELIMITER)[2]
        }
    }

    inner class ConfiguredTask(private val configuredContext: ConfiguredContext) : ConfigurableJobService.IConfiguredTask {
        @Inject
        lateinit var authManager: OTAuthManager

        @field:[Inject Backend]
        lateinit var realmFactory: Factory<Realm>

        @Inject
        lateinit var syncServerController: ISynchronizationServerSideAPI

        @field:[Inject ConfiguredObject]
        lateinit var preferences: SharedPreferences

        private val subscriptionDict = Hashtable<String, Disposable>()

        init {
            configuredContext.configuredAppComponent.inject(this)
        }

        private fun setSubscription(tag: String, disposable: Disposable) {
            subscriptionDict[tag]?.dispose()
            subscriptionDict[tag] = disposable
        }

        override fun dispose() {
            subscriptionDict.forEach { it.value?.dispose() }
            subscriptionDict.clear()
        }


        override fun onStartJob(job: JobParameters): Boolean {
            val informationType = getInformationType(job.tag)
            if (authManager.isUserSignedIn() && subscriptionDict[informationType]?.isDisposed != false) {
                preferences.edit().putBoolean(informationType, true).apply()
                when (informationType) {
                    INFORMATION_DEVICE -> {
                        setSubscription(informationType,
                                OTDeviceInfo.makeDeviceInfo(configuredContext.firebaseComponent).flatMap { deviceInfo ->
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
            subscriptionDict[getInformationType(job.tag)]?.dispose()
            subscriptionDict.remove(job.tag)
            return !preferences.getBoolean(getInformationType(job.tag), false)
        }

    }


    override fun makeNewTask(configuredContext: ConfiguredContext): ConfiguredTask {
        return ConfiguredTask(configuredContext)
    }

    override fun extractConfigIdOfJob(job: JobParameters): String {
        return getConfigId(job.tag)
    }
}