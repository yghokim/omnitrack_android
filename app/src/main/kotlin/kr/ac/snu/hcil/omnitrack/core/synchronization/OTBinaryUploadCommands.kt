package kr.ac.snu.hcil.omnitrack.core.synchronization

import android.content.Context
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.android.common.file.FileHelper
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.di.global.BinaryStorageServer
import kr.ac.snu.hcil.omnitrack.core.net.IBinaryStorageCore
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

class OTBinaryUploadCommands(val context: Context) {

    companion object {
        const val TAG = "OTBinaryUploadWork"
        const val NOTIFICATION_IDENTIFIER: Int = 903829784
    }

    @Inject
    lateinit var core: IBinaryStorageCore

    @field:[Inject BinaryStorageServer]
    lateinit var realmFactory: Factory<Realm>

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    fun createWork(useProgressNotification: Boolean = true): Completable {
        return crawlAndUpload().run {
            return@run if (useProgressNotification) {
                this.doOnSubscribe {
                    context.runOnUiThread {
                        notificationManager.notify(TAG, NOTIFICATION_IDENTIFIER,
                                OTTaskNotificationManager.makeTaskProgressNotificationBuilder(context,
                                        context.getString(R.string.msg_uploading_file_to_server), context.getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                                        null, R.drawable.icon_cloud_upload).build())

                    }
                }.doFinally {
                    context.runOnUiThread {
                        notificationManager.cancel(TAG, NOTIFICATION_IDENTIFIER)
                    }
                }
            } else this
        }
    }

    private fun getUploadTaskAndIncreaseCount(maxTrialCount: Int): List<UploadTaskInfo> {
        val realm = realmFactory.get()
        val currentUploadTasks = realm.copyFromRealm(realm.where(UploadTaskInfo::class.java).findAll().filter { it.trialCount.get() ?: 0L < maxTrialCount })
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
        realm.close()
        return currentUploadTasks
    }

    private fun convertUploadTask(uploadTask: UploadTaskInfo): Single<Boolean> {
        return core.startNewUploadTaskImpl(uploadTask
        ) { sessionUri ->
            realmFactory.get().use {
                it.executeTransaction {
                    uploadTask.sessionUri = sessionUri
                    it.copyToRealmOrUpdate(uploadTask)
                }
            }
        }.observeOn(Schedulers.io()).doOnComplete {
            realmFactory.get().use {
                val cacheEntry = it.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", uploadTask.serverPath).findFirst()
                it.executeTransaction { transactionRealm ->
                    cacheEntry?.synchronizedAt = System.currentTimeMillis()
                    transactionRealm.where(UploadTaskInfo::class.java).equalTo("id", uploadTask.id).findFirst()?.deleteFromRealm()
                }
            }
        }.doOnError { err ->
            println("binary upload error")
            err.printStackTrace()
        }.toSingle { true }.onErrorReturn { false }
    }

    private fun crawlAndUpload(maxTrialCount: Int = Int.MAX_VALUE): Completable {
        return Completable.defer {
            val uploadTasks = getUploadTaskAndIncreaseCount(maxTrialCount)
            if (uploadTasks.isEmpty()) {
                return@defer Completable.complete()
            } else {
                println("continue with ${uploadTasks.count()} upload tasks...")
                Single.zip(
                        uploadTasks.map { uploadTaskInfo -> convertUploadTask(uploadTaskInfo) }) { resultSet ->
                    Pair(resultSet.count { it == true }, resultSet.size)
                }.flatMapCompletable { (success, total) ->
                    return@flatMapCompletable if (success == total) {
                        crawlAndUpload(0)
                    } else Completable.error(Exception("Some upload tasks were failed - ${total - success} of $total tasks failed."))
                }
            }
        }
    }
}