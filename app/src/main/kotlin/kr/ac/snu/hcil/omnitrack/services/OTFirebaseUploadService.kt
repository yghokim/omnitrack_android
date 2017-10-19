package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.ABinaryUploadService
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by younghokim on 2017. 3. 14..
 */
class OTFirebaseUploadService : ABinaryUploadService(TAG) {

    class ServiceController(context: Context) : ABinaryUploadServiceController(OTFirebaseUploadService::class.java, context) {
        override fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri =
                Uri.parse(getItemStorageReference(itemId, trackerId, userId).child(fileName).path)

    }

    companion object {

        const val TAG = "FilebaseUploadService"

        //string: Realm id of taskInfo
        private val currentTasks = ConcurrentHashMap<String, UploadTask>()

        fun getItemStorageReference(itemId: String, trackerId: String, userId: String): StorageReference {
            return FirebaseStorage.getInstance().reference.child("entry_data").child(userId).child(trackerId).child(itemId)
        }
    }

    override fun isTaskOngoing(taskInfo: UploadTaskInfo): Boolean {
        return currentTasks.containsKey(taskInfo.id)
    }

    override fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit, finished: () -> Unit) {
        val itemId = taskInfo.itemId
        val userId = taskInfo.userId
        val trackerId = taskInfo.trackerId
        val localUri = taskInfo.localUriCompat()

        val storageRef = getItemStorageReference(itemId, trackerId, userId).child(localUri.lastPathSegment)

        val task = storageRef.putFile(localUri, StorageMetadata.Builder().build(), if (taskInfo.sessionUri != null) Uri.parse(taskInfo.sessionUri!!) else null)

        currentTasks[taskInfo.id] = task

        task.addOnProgressListener { snapshot: UploadTask.TaskSnapshot ->
            val sessionUri = snapshot.uploadSessionUri?.toString()
            println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

            if (sessionUri != null) {
                onProgress.invoke(sessionUri)
            }
        }.addOnCompleteListener {
            currentTasks.remove(taskInfo.id)
            finished.invoke()
        }
    }
}