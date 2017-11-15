package kr.ac.snu.hcil.omnitrack.services

import android.content.Intent
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.IBinaryStorageCore
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 26..
 */
class OTBinaryUploadService : WakefulService(TAG) {

    companion object {
        const val TAG = "OTBinaryUploadService"
        const val NOTIFICATION_IDENTIFIER: Int = 903829784

        const val ACTION_UPLOAD = "omnitrack_binary_upload"
        const val ACTION_RESUME = "omnitrack_restart_binary_uploads"

        const val EXTRA_OUT_URI = "BinaryUploadService_outUri"

        private val realmConfiguration: RealmConfiguration by lazy {
            RealmConfiguration.Builder().name("uploadTask").deleteRealmIfMigrationNeeded().build()
        }
    }

    @Inject
    lateinit var core: IBinaryStorageCore

    private val ongoingTaskIds = HashSet<String>()

    private val subscriptions = CompositeDisposable()
    private lateinit var realm: Realm

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).networkComponent.inject(this)
        realm = Realm.getInstance(realmConfiguration)
    }

    override fun onDestroy() {
        realm.close()
        subscriptions.clear()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            ACTION_RESUME -> {
                println("resume upload if cached....")
                resumeCachedUploads()
                return START_NOT_STICKY
            }
            ACTION_UPLOAD -> {
                println("start upload....")
                return startNewUploadTask(intent, startId)
            }
            else -> return START_NOT_STICKY
        }

    }

    protected fun isTaskOngoing(taskInfo: UploadTaskInfo): Boolean = ongoingTaskIds.contains(taskInfo.id)

    private fun resumeCachedUploads() {
        val tasks = realm.where(UploadTaskInfo::class.java).findAll()
        println("${tasks.size} upload tasks were hanging.")

        for (taskInfo in tasks) {
            if (!isTaskOngoing(taskInfo)) {
                println("restart uploading ${taskInfo.localUri}")
                subscriptions.add(
                        core.startNewUploadTaskImpl(taskInfo, { sessionUri ->
                            realm.executeTransaction {
                                taskInfo.sessionUri = sessionUri
                            }
                        }).subscribe({
                            realm.executeTransaction {
                                realm.where(UploadTaskInfo::class.java).equalTo("id", taskInfo.id).findAll().deleteAllFromRealm()
                            }
                        }, { err ->
                            println("A resumed binary upload task was failed")
                            err.printStackTrace()
                        }))
            }
        }

    }

    private fun startNewUploadTask(intent: Intent, startId: Int): Int {
        if (intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
                && intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
                && intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER)
                && intent.hasExtra(EXTRA_OUT_URI)) {

            val itemId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
            val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
            val userId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER)
            val outUri = SynchronizedUri.parser.fromJson(intent.getStringExtra(EXTRA_OUT_URI), SynchronizedUri::class.java)

            Toast.makeText(this, R.string.msg_uploading_file_to_server, Toast.LENGTH_SHORT).show()

            val notification = OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this,
                    getString(R.string.msg_uploading_file_to_server), getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                    null, R.drawable.icon_cloud_upload).build()
            startForeground(NOTIFICATION_IDENTIFIER, notification)

            println("SynchronizedUri: ${outUri}")
            println("local uri info: ${outUri.localUri.scheme}, isAbsolute: ${outUri.localUri.isAbsolute}, isRelative: ${outUri.localUri.isRelative}, scheme: ${outUri.localUri.scheme}")

            val dbObject = UploadTaskInfo()
            dbObject.id = UUID.randomUUID().toString()
            ongoingTaskIds.add(dbObject.id)

            //realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
            dbObject.itemId = itemId
            dbObject.trackerId = trackerId
            dbObject.userId = userId
            dbObject.localUri = outUri.localUri.toString()
            dbObject.serverUri = outUri.serverUri.toString()

            realm.executeTransaction {
                realm.copyToRealmOrUpdate(dbObject)
            }

            subscriptions.add(
                    core.startNewUploadTaskImpl(dbObject,
                            { sessionUri ->
                                realm.executeTransaction {
                                    dbObject.sessionUri = sessionUri
                                    realm.copyToRealmOrUpdate(dbObject)
                                }
                            }).doAfterTerminate {

                        stopSelf(startId)

                        if (realm.where(UploadTaskInfo::class.java).count() == 0L) {
                            stopSelf()
                        }
                    }.subscribe({
                        ongoingTaskIds.remove(dbObject.id)

                        realm.executeTransaction {
                            realm.where(UploadTaskInfo::class.java).equalTo("id", dbObject.id).findAll().deleteAllFromRealm()
                        }

                    }, { err ->
                        println("binary upload error")
                        err.printStackTrace()
                    })
            )

        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

}