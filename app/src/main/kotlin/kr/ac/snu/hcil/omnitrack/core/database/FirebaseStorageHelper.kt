package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.realm.Realm
import java.util.*


/**
 * Created by Young-Ho on 3/4/2017.
 */
class FirebaseStorageHelper(context: Context) {

    private val currentTasks = ArrayList<Pair<UploadTask, String>>()

    init {
        Realm.init(context)

        val realm = Realm.getDefaultInstance()
        val tasks = realm.where(UploadTaskInfo::class.java).findAll()
        println("${tasks.size} upload tasks were hanging.")

        for (taskInfo in tasks) {
            println("restart uploading ${taskInfo.localUri}")
            val localUri = Uri.parse(taskInfo.localUri)
            val storageRef = getItemStorageReference(taskInfo.itemId, taskInfo.trackerId, taskInfo.userId).child(localUri.lastPathSegment)

            val task = storageRef.putFile(localUri, StorageMetadata.Builder().build(), if (taskInfo.sessionUri != null) Uri.parse(taskInfo.sessionUri!!) else null)
            val pair = Pair(task, taskInfo.id)
            currentTasks.add(pair)

            task.addOnProgressListener {
                snapshot: UploadTask.TaskSnapshot ->
                println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

                realm.beginTransaction()
                taskInfo.sessionUri = snapshot.uploadSessionUri?.toString()
                realm.commitTransaction()
            }.addOnCompleteListener {

                println("image upload complete.")

                realm.beginTransaction()
                taskInfo.deleteFromRealm()
                realm.commitTransaction()
                currentTasks.remove(pair)
            }

        }
    }

    fun restartUploadTask() {


    }

    fun getItemStorageReference(itemId: String, trackerId: String, userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child("entry_data").child(userId).child(trackerId).child(itemId)
    }

    fun assignNewUploadTask(outUri: SynchronizedUri, itemId: String, trackerId: String, userId: String) {

        val storageRef = getItemStorageReference(itemId, trackerId, userId).child(outUri.localUri.lastPathSegment)

        println("upload uri to ${storageRef.path}")
        outUri.setSynchronized(Uri.parse(storageRef.path))
        val task = storageRef.putFile(outUri.localUri)


        if (currentTasks.filter { it.first === task }.isEmpty()) {
            val realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            val dbObject = realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
            dbObject.itemId = itemId
            dbObject.trackerId = trackerId
            dbObject.userId = userId
            dbObject.localUri = outUri.localUri.toString()
            dbObject.serverUri = outUri.serverUri.toString()

            realm.commitTransaction()

            val pair = Pair(task, dbObject.id)

            currentTasks.add(pair)

            task.addOnProgressListener {
                snapshot: UploadTask.TaskSnapshot ->
                println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

                realm.beginTransaction()
                dbObject.sessionUri = snapshot.uploadSessionUri?.toString()
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
                println("image upload complete.")
                if (task.isSuccessful) {
                    println("result: success")
                    //upload success
                } else {
                    //fail
                    println("result: fail")
                }

                realm.beginTransaction()
                dbObject.deleteFromRealm()
                realm.commitTransaction()
                currentTasks.remove(pair)
            }
        }
    }

}