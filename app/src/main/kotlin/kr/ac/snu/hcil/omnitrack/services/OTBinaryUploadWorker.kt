package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.widget.Toast
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.IBinaryStorageCore
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTBinaryUploadWorker(val context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams) {

    companion object {
        const val TAG = "OTBinaryUploadWork"
        const val NOTIFICATION_IDENTIFIER: Int = 903829784
    }

    override fun getBackgroundScheduler(): Scheduler {
        return realmScheduler
    }

    @Inject
    lateinit var core: IBinaryStorageCore

    @Inject
    lateinit var controller: OTBinaryStorageController

    private val realmScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    init {
        (context.applicationContext as OTAndroidApp).currentConfiguredContext.configuredAppComponent.inject(this)
    }

    override fun createWork(): Single<Result> {
        return Single.defer<Result> {
            return@defer crawlAndUpload()
        }.subscribeOn(realmScheduler).doOnSubscribe {
            context.runOnUiThread {
                notificationManager.notify(TAG, NOTIFICATION_IDENTIFIER,
                        OTTaskNotificationManager.makeTaskProgressNotificationBuilder(context,
                                context.getString(R.string.msg_uploading_file_to_server), context.getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                                null, R.drawable.icon_cloud_upload).build())

                Toast.makeText(context, context.getString(R.string.msg_uploading_file_to_server), Toast.LENGTH_LONG).show()
            }
        }.doFinally {
            context.runOnUiThread {
                notificationManager.cancel(TAG, NOTIFICATION_IDENTIFIER)
            }
        }
    }

    private fun crawlAndUpload(maxTrialCount: Int = Int.MAX_VALUE): Single<Result> {
        return Single.defer<Result> {
            val realm = controller.realmProvider.get()
            val currentUploadTasks = realm.where(UploadTaskInfo::class.java).findAll().filter { it.trialCount.get() ?: 0L < maxTrialCount }.toMutableList()
            val localInvalidTasks = currentUploadTasks.filter { it ->
                !FileHelper.exists(it.localFilePath)
            }
            currentUploadTasks.removeAll(localInvalidTasks)
            realm.executeTransaction {
                localInvalidTasks.forEach { it.deleteFromRealm() }
                currentUploadTasks.forEach {
                    if (it.trialCount.isNull) {
                        it.trialCount.set(0)
                    } else it.trialCount.increment(1)
                }
            }

            if (currentUploadTasks.isEmpty()) {
                realm.close()
                return@defer Single.just(Result.success())
            } else {
                println("continue with ${currentUploadTasks.count()} upload tasks...")
                Single.zip(
                        currentUploadTasks.map { dbObject ->
                            core.startNewUploadTaskImpl(dbObject
                            ) { sessionUri ->
                                realm.executeTransaction {
                                    dbObject.sessionUri = sessionUri
                                }
                            }.observeOn(realmScheduler).doOnComplete {
                                val cacheEntry = realm.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", dbObject.serverPath).findFirst()
                                realm.executeTransaction {
                                    cacheEntry?.synchronizedAt = System.currentTimeMillis()
                                    dbObject.deleteFromRealm()
                                }
                            }.doOnError { err ->
                                println("binary upload error")
                                err.printStackTrace()
                            }.toSingle { true }.onErrorReturn { false }
                        }) {
                    if (it.any { it == false }) {
                        Result.retry()
                    } else {
                        Result.success()
                    }
                }.doFinally {
                    realm.close()
                }.flatMap {
                    when (it) {
                        is Result.Success ->
                            crawlAndUpload(0)
                        else ->
                            Single.just(it)
                    }
                }
            }
        }.subscribeOn(realmScheduler)
    }

}