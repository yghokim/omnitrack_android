package kr.ac.snu.hcil.omnitrack.services

import android.widget.Toast
import com.firebase.jobdispatcher.JobParameters
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.IBinaryStorageCore
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTBinaryUploadService : ConfigurableJobService() {
    companion object {
        const val TAG = "OTBinaryUploadService"
        const val NOTIFICATION_IDENTIFIER: Int = 903829784

        const val JOB_TAG_DELIMITER = ';'
        const val ACTION_UPLOAD = "omnitrack_binary_upload"
        const val ACTION_RESUME = "omnitrack_restart_binary_uploads"

        const val EXTRA_OUT_URI = "BinaryUploadService_outUri"

        fun makeTag(configurationId: String, action: String): String {
            return "$configurationId;$action"
        }

        fun getConfigId(tag: String): String {
            return tag.split(JOB_TAG_DELIMITER)[0]
        }

        fun getInformationType(tag: String): String {
            return tag.split(JOB_TAG_DELIMITER)[1]
        }
    }

    inner class ConfiguredTask(private val configuredContext: ConfiguredContext) : ConfigurableJobService.IConfiguredTask {

        @Inject
        lateinit var core: IBinaryStorageCore

        @Inject
        lateinit var controller: OTBinaryStorageController

        private val realm: Realm

        private val subscriptions = CompositeDisposable()

        init {
            configuredContext.configuredAppComponent.inject(this)
            realm = controller.realmProvider.get()
        }

        override fun dispose() {
            realm.close()
            subscriptions.clear()
        }

        override fun onStopJob(job: JobParameters): Boolean {
            //Network aborted, etc.
            subscriptions.clear()
            controller.realmProvider.get().use { realm ->
                if (realm.where(UploadTaskInfo::class.java).count() == 0L) {
                    return false
                } else {
                    println("Upload Service Error. retry later.")
                    return true
                }
            }
        }

        override fun onStartJob(job: JobParameters): Boolean {
            notificationManager.notify(TAG, NOTIFICATION_IDENTIFIER,
                    OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this@OTBinaryUploadService,
                            getString(R.string.msg_uploading_file_to_server), getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                            null, R.drawable.icon_cloud_upload).build())

            Toast.makeText(this@OTBinaryUploadService, getString(R.string.msg_uploading_file_to_server), Toast.LENGTH_LONG).show()

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
                                                this@OTBinaryUploadService.runOnUiThread {
                                                    realm.executeTransaction {
                                                        dbObject.sessionUri = sessionUri
                                                        realm.insertOrUpdate(dbObject)
                                                    }
                                                }
                                            }).doOnComplete {

                                        this@OTBinaryUploadService.runOnUiThread {
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

    override fun makeNewTask(configuredContext: ConfiguredContext): ConfiguredTask {
        return ConfiguredTask(configuredContext)
    }

    override fun extractConfigIdOfJob(job: JobParameters): String {
        return getConfigId(job.tag)
    }

}