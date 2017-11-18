package kr.ac.snu.hcil.omnitrack.services

import android.widget.Toast
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.IBinaryStorageCore
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTBinaryUploadService : JobService() {
    companion object {
        const val TAG = "OTBinaryUploadService"
        const val NOTIFICATION_IDENTIFIER: Int = 903829784

        const val ACTION_UPLOAD = "omnitrack_binary_upload"
        const val ACTION_RESUME = "omnitrack_restart_binary_uploads"

        const val EXTRA_OUT_URI = "BinaryUploadService_outUri"

    }

    @Inject
    lateinit var core: IBinaryStorageCore

    @Inject
    lateinit var controller: OTBinaryStorageController

    private lateinit var realm: Realm

    private val ongoingTaskIds = HashSet<String>()

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).networkComponent.inject(this)
        realm = controller.realmProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
        subscriptions.clear()
    }

    override fun onStopJob(job: JobParameters?): Boolean {
        //Network aborted, etc.
        subscriptions.clear()
        if (realm.where(UploadTaskInfo::class.java).findAll().isEmpty()) {
            return false
        } else {
            println("Upload Service Error. retry later.")
            return true
        }
    }

    override fun onStartJob(job: JobParameters): Boolean {
        notificationManager.notify(TAG, NOTIFICATION_IDENTIFIER,
                OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this,
                        getString(R.string.msg_uploading_file_to_server), getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                        null, R.drawable.icon_cloud_upload).build())

        Toast.makeText(this, getString(R.string.msg_uploading_file_to_server), Toast.LENGTH_LONG).show()

        return crawlAndUpload(job)
    }

    private fun crawlAndUpload(job: JobParameters, maxTrialCount: Int = Int.MAX_VALUE): Boolean {

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
            println("task is empty. finish download service.")
            notificationManager.cancel(TAG, NOTIFICATION_IDENTIFIER)

            jobFinished(job, realm.where(UploadTaskInfo::class.java).count() > 0)
            return false
        } else {
            println("continue with ${currentUploadTasks.count()} upload tasks...")
            subscriptions.add(
                    Completable.merge(
                            currentUploadTasks.map { dbObject ->
                                core.startNewUploadTaskImpl(dbObject,
                                        { sessionUri ->
                                            this.runOnUiThread {
                                                realm.executeTransaction {
                                                    dbObject.sessionUri = sessionUri
                                                    realm.copyToRealmOrUpdate(dbObject)
                                                }
                                            }
                                        }).doOnComplete {

                                    this.runOnUiThread {
                                        ongoingTaskIds.remove(dbObject.id)

                                        val cacheEntry = realm.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", dbObject.serverPath).findFirst()
                                        realm.executeTransaction {
                                            cacheEntry?.synchronizedAt = System.currentTimeMillis()
                                            dbObject.deleteFromRealm()
                                        }
                                    }

                                }.doOnError { err ->
                                    println("binary upload error")
                                    err.printStackTrace()
                                }.onErrorComplete()
                            }
                    ).observeOn(AndroidSchedulers.mainThread()).subscribe({
                        crawlAndUpload(job, 0) // retry if fresh tasks inserted.
                    })
            )
            return true
        }
    }

}