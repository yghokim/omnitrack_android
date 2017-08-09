package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by younghokim on 2017. 3. 14..
 */
class OTFirebaseUploadService : WakefulService(TAG) {

    companion object {

        const val TAG = "FilebaseUploadService"
        const val NOTIFICATION_IDENTIFIER = 1

        const val ACTION_UPLOAD = "omnitrack_firebase_upload"
        const val ACTION_RESUME = "omnitrack_restart_firebase_uploads"

        const val EXTRA_OUT_URI = "firebaseUploadService_outUri"

        //string: Realm id of taskInfo
        private val currentTasks = ConcurrentHashMap<String, UploadTask>()


        private val realmConfiguration: RealmConfiguration by lazy {
            RealmConfiguration.Builder().name("uploadTask").deleteRealmIfMigrationNeeded().build()
        }


        fun makeUploadTaskIntent(context: Context, outUri: SynchronizedUri, itemId: String, trackerId: String, userId: String): Intent {
            return Intent(context, OTFirebaseUploadService::class.java)
                    .setAction(ACTION_UPLOAD)
                    .putExtra(EXTRA_OUT_URI, SynchronizedUri.parser.toJson(outUri))
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, userId)
        }

        fun makeResumeUploadIntent(context: Context): Intent {
            return Intent(context, OTFirebaseUploadService::class.java)
                    .setAction(ACTION_RESUME)
        }

        fun getItemStorageReference(itemId: String, trackerId: String, userId: String): StorageReference {
            return FirebaseStorage.getInstance().reference.child("entry_data").child(userId).child(trackerId).child(itemId)
        }
    }


    override fun onCreate() {
        super.onCreate()
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

    private fun resumeCachedUploads() {
        val realm = Realm.getInstance(realmConfiguration)
        val tasks = realm.where(UploadTaskInfo::class.java).findAll()
        println("${tasks.size} upload tasks were hanging.")

        for (taskInfo in tasks) {

            if (!currentTasks.containsKey(taskInfo.id)) {
                println("restart uploading ${taskInfo.localUri}")
                var localUri = Uri.parse(taskInfo.localUri)
                if (localUri.scheme == null) {
                    localUri = Uri.Builder()
                            .scheme("file")
                            .path(localUri.path)
                            .build()
                }
                val storageRef = getItemStorageReference(taskInfo.itemId, taskInfo.trackerId, taskInfo.userId).child(localUri.lastPathSegment)

                val task = storageRef.putFile(localUri, StorageMetadata.Builder().build(), if (taskInfo.sessionUri != null) Uri.parse(taskInfo.sessionUri!!) else null)
                currentTasks[taskInfo.id] = task

                task.addOnProgressListener {
                    snapshot: UploadTask.TaskSnapshot ->
                    println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

                    realm.beginTransaction()
                    taskInfo.sessionUri = snapshot.uploadSessionUri?.toString()
                    realm.copyToRealmOrUpdate(taskInfo)
                    realm.commitTransaction()
                }.addOnCompleteListener {

                    println("file upload complete.")
                    currentTasks.remove(taskInfo.id)
                    realm.beginTransaction()
                    realm.where(UploadTaskInfo::class.java).equalTo("id", taskInfo.id).findAll().deleteAllFromRealm()
                    realm.commitTransaction()
                }
            }
        }

    }

    private fun startNewUploadTask(intent: Intent, startId: Int): Int {
        if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
                && intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                && intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)
                && intent.hasExtra(EXTRA_OUT_URI)) {

            val itemId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)
            val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
            val userId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER)
            val outUri = SynchronizedUri.parser.fromJson(intent.getStringExtra(EXTRA_OUT_URI), SynchronizedUri::class.java)

            val storageRef = getItemStorageReference(itemId, trackerId, userId).child(outUri.localUri.lastPathSegment)

            println("upload uri to ${storageRef.path}")

            Toast.makeText(this, R.string.msg_uploading_file_to_server, Toast.LENGTH_SHORT).show()

            OTTaskNotificationManager.setTaskProgressNotification(this, TAG, NOTIFICATION_IDENTIFIER,
                    getString(R.string.msg_uploading_file_to_server), getString(R.string.msg_uploading), OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                    R.drawable.icon_cloud_upload, R.drawable.icon_cloud_upload)


            println("local uri info: ${outUri.localUri.scheme}, isAbsolute: ${outUri.localUri.isAbsolute}, isRelative: ${outUri.localUri.isRelative}, scheme: ${outUri.localUri.scheme}")

            val task = if (outUri.localUri.scheme == null) {
                storageRef.putFile(Uri.Builder().scheme("file").path(outUri.localUri.path).build())
            } else {
                storageRef.putFile(outUri.localUri)
            }

            val realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            val dbObject = UploadTaskInfo()
            dbObject.id = UUID.randomUUID().toString()

            //realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
            dbObject.itemId = itemId
            dbObject.trackerId = trackerId
            dbObject.userId = userId
            dbObject.localUri = outUri.localUri.toString()
            dbObject.serverUri = outUri.serverUri.toString()

            realm.copyToRealmOrUpdate(dbObject)

            realm.commitTransaction()

            currentTasks[dbObject.id] = task

            task.addOnProgressListener {
                snapshot: UploadTask.TaskSnapshot ->
                println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

                realm.beginTransaction()
                dbObject.sessionUri = snapshot.uploadSessionUri?.toString()
                realm.copyToRealmOrUpdate(dbObject)
                realm.commitTransaction()

            }.addOnFailureListener {
                ex ->
                println("uploadtask failed")
                ex.printStackTrace()
            }.addOnPausedListener {
                taskSnapshot ->
                println("paused: ${taskSnapshot.uploadSessionUri?.toString()}")
            }.addOnCompleteListener {
                task ->
                println("file upload complete.")
                if (task.isSuccessful) {
                    println("result: success")
                    //upload success
                } else {
                    //fail
                    println("result: fail")
                }

                currentTasks.remove(dbObject.id)
                realm.beginTransaction()
                realm.where(UploadTaskInfo::class.java).equalTo("id", dbObject.id).findAll().deleteAllFromRealm()
                realm.commitTransaction()
                stopSelf(startId)
                finishIfAllTasksDone()
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }


    fun finishIfAllTasksDone() {
        if (currentTasks.isEmpty()) {
            OTTaskNotificationManager.dismissNotification(this, NOTIFICATION_IDENTIFIER, TAG)
            stopSelf()
        }
    }

}