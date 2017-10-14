package kr.ac.snu.hcil.omnitrack.core.database.abstraction

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.services.WakefulService
import java.util.*

/**
 * Created by younghokim on 2017. 9. 26..
 */
abstract class ABinaryUploadService(tag: String) : WakefulService(tag) {

    abstract class ABinaryUploadServiceController(val serviceClass: Class<*>, val context: Context) {
        fun makeUploadServiceIntent(outUri: SynchronizedUri, itemId: String, trackerId: String, userId: String): Intent {
            return Intent(context, serviceClass)
                    .setAction(ACTION_UPLOAD)
                    .putExtra(EXTRA_OUT_URI, SynchronizedUri.parser.toJson(outUri))
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, userId)
        }


        fun makeResumeUploadIntent(): Intent {
            return Intent(context, serviceClass)
                    .setAction(ACTION_RESUME)
        }

        abstract fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri
    }

    companion object {

        const val NOTIFICATION_IDENTIFIER: Int = 903829784

        const val ACTION_UPLOAD = "omnitrack_binary_upload"
        const val ACTION_RESUME = "omnitrack_restart_binary_uploads"

        const val EXTRA_OUT_URI = "BinaryUploadService_outUri"

        private val realmConfiguration: RealmConfiguration by lazy {
            RealmConfiguration.Builder().name("uploadTask").deleteRealmIfMigrationNeeded().build()
        }
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

    protected open fun isTaskOngoing(taskInfo: UploadTaskInfo): Boolean = true

    private fun resumeCachedUploads() {
        val realm = Realm.getInstance(realmConfiguration)
        val tasks = realm.where(UploadTaskInfo::class.java).findAll()
        println("${tasks.size} upload tasks were hanging.")

        for (taskInfo in tasks) {

            if (!isTaskOngoing(taskInfo)) {
                println("restart uploading ${taskInfo.localUri}")

                startNewUploadTaskImpl(taskInfo, { sessionUri ->
                    realm.executeTransaction {
                        taskInfo.sessionUri = sessionUri
                        realm.copyToRealmOrUpdate(taskInfo)
                    }
                }, {
                    realm.executeTransaction {
                        realm.where(UploadTaskInfo::class.java).equalTo("id", taskInfo.id).findAll().deleteAllFromRealm()
                        realm.commitTransaction()
                    }
                })
            }
        }

    }

    protected abstract fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit, finished: () -> Unit)

    private fun startNewUploadTask(intent: Intent, startId: Int): Int {
        if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
                && intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                && intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)
                && intent.hasExtra(EXTRA_OUT_URI)) {

            val itemId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
            val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
            val userId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)
            val outUri = SynchronizedUri.parser.fromJson(intent.getStringExtra(EXTRA_OUT_URI), SynchronizedUri::class.java)

            Toast.makeText(this, R.string.msg_uploading_file_to_server, Toast.LENGTH_SHORT).show()

            val notification = OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this,
                    getString(R.string.msg_uploading_file_to_server), getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                    null, R.drawable.icon_cloud_upload).build()
            startForeground(NOTIFICATION_IDENTIFIER, notification)

            println("SynchronizedUri: ${outUri}")
            println("local uri info: ${outUri.localUri.scheme}, isAbsolute: ${outUri.localUri.isAbsolute}, isRelative: ${outUri.localUri.isRelative}, scheme: ${outUri.localUri.scheme}")

            val realm = Realm.getDefaultInstance()
            val dbObject = UploadTaskInfo()
            dbObject.id = UUID.randomUUID().toString()

            //realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
            dbObject.itemId = itemId
            dbObject.trackerId = trackerId
            dbObject.userId = userId
            dbObject.localUri = outUri.localUri.toString()
            dbObject.serverUri = outUri.serverUri.toString()

            realm.executeTransaction {
                realm.copyToRealmOrUpdate(dbObject)
            }

            startNewUploadTaskImpl(dbObject,
                    { sessionUri ->
                        realm.executeTransaction {
                            dbObject.sessionUri = sessionUri
                            realm.copyToRealmOrUpdate(dbObject)
                        }
                    },
                    {
                        realm.executeTransaction {
                            realm.where(UploadTaskInfo::class.java).equalTo("id", dbObject.id).findAll().deleteAllFromRealm()
                        }
                        stopSelf(startId)

                        if (realm.where(UploadTaskInfo::class.java).count() == 0L) {
                            stopSelf()
                        }
                    })

        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

}