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
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.Backend
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

    @Inject
    lateinit var preferences: SharedPreferences

    private val subscriptionDict = Hashtable<String, Disposable>()

    private fun setSubscription(tag: String, disposable: Disposable) {
        subscriptionDict[tag]?.dispose()
        subscriptionDict[tag] = disposable
    }

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionDict.forEach { it.value?.dispose() }
        subscriptionDict.clear()

    }

    override fun onStopJob(job: JobParameters): Boolean {
        subscriptionDict[job.tag]?.dispose()
        subscriptionDict.remove(job.tag)
        return !preferences.getBoolean(job.tag, false)
    }

    override fun onStartJob(job: JobParameters): Boolean {
        if (authManager.isUserSignedIn() && subscriptionDict[job.tag]?.isDisposed != false) {
            preferences.edit().putBoolean(job.tag, true).apply()
            when (job.tag) {
                INFORMATION_DEVICE -> {
                    setSubscription(job.tag, syncServerController
                            .putDeviceInfo(OTDeviceInfo()).doFinally {
                        subscriptionDict.remove(job.tag)
                    }.subscribe({ deviceInfoResult ->
                        preferences.edit().putBoolean(job.tag, false).apply()
                        jobFinished(job, false)
                    }, { err ->
                        jobFinished(job, true)
                    })
                    )
                }
                INFORMATION_USERNAME -> {
                    val uid = authManager.userId
                    if (uid != null) {
                        setSubscription(job.tag, Maybe.defer {
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
}