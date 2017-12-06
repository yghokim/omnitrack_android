package kr.ac.snu.hcil.omnitrack.services

import android.content.SharedPreferences
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 12. 5..
 */
class OTInformationUploadService : JobService() {

    companion object {
        const val INFORMATION_DEVICE = "deviceInfo"
    }

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var syncServerController: ISynchronizationServerSideAPI

    @Inject
    lateinit var preferences: SharedPreferences

    private val subscriptionDict = Hashtable<String, Disposable>()

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
                    subscriptionDict[job.tag] = syncServerController
                            .putDeviceInfo(OTDeviceInfo()).doFinally {
                        subscriptionDict.remove(job.tag)
                    }.subscribe({ deviceInfoResult ->
                        preferences.edit().putBoolean(job.tag, false).apply()
                        jobFinished(job, false)
                    }, { err ->
                        jobFinished(job, true)
                    })
                }
            }
            return true
        } else return false
    }

}