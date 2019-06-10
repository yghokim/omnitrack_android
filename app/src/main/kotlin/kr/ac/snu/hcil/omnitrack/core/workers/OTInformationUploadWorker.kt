package kr.ac.snu.hcil.omnitrack.core.workers

import android.content.Context
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import dagger.internal.Factory
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 12. 5..
 */
class OTInformationUploadWorker(private val context: Context, private val workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    companion object {
        const val TAG = "OTInformationUploadWorker"
        const val KEY_TYPE = "type"
        const val INFORMATION_DEVICE = "deviceInfo"
        const val INFORMATION_USERNAME = "userName"
    }

    @Inject
    lateinit var authManager: OTAuthManager

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var syncServerController: ISynchronizationServerSideAPI

    private val app: OTAndroidApp = context.applicationContext as OTAndroidApp


    private val realmScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    override fun getBackgroundScheduler(): Scheduler {
        return realmScheduler
    }

    init {
        app.applicationComponent.inject(this)
    }

    override fun createWork(): Single<Result> {
        return Single.defer {
            val informationType = workerParams.inputData.getString(KEY_TYPE)
            if (authManager.isUserSignedIn()) {
                val uid = authManager.userId!!
                when (informationType) {
                    INFORMATION_DEVICE ->
                        OTDeviceInfo.makeDeviceInfo(context).flatMap { deviceInfo ->
                            syncServerController
                                    .putDeviceInfo(deviceInfo)
                        }.map { Result.success() }
                    /*
                    INFORMATION_USERNAME ->
                        Single.defer {
                            val realm = realmFactory.get()
                            val userInfo = realm.where<OTUserDAO>().equalTo("uid", uid).findFirst()
                            if (userInfo != null) {
                                syncServerController.putUserName(userInfo.name, userInfo.nameUpdatedAt)
                                        .observeOn(backgroundScheduler)
                                        .map { result ->
                                            if (result.success) {
                                                realm.executeTransaction {
                                                    userInfo.name = result.finalValue
                                                    userInfo.nameSynchronizedAt = result.payloads?.get("updatedAt")?.toLong()
                                                    userInfo.nameSynchronizedAt?.let {
                                                        userInfo.nameUpdatedAt = it
                                                    }
                                                }
                                                println("$TAG: Updated UserInfo to " + result.finalValue)
                                                Result.success()
                                            } else {
                                                Result.failure()
                                            }
                                        }.onErrorReturn { err ->
                                            err.printStackTrace()
                                            Result.failure()
                                        }.doFinally { realm.close() }
                            } else {
                                println("$TAG: there is no userinfo.")
                                realm.close()
                                Single.just(Result.failure())
                            }
                        }*/
                    else -> Single.just(Result.failure())
                }
            } else Single.just(Result.failure())
        }.subscribeOn(backgroundScheduler)
    }

}