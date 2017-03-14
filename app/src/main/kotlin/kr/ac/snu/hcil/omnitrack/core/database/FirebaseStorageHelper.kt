package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StreamDownloadTask
import rx.Single
import rx.subscriptions.Subscriptions
import java.io.File
import java.io.InputStream


/**
 * Created by Young-Ho on 3/4/2017.
 */
class FirebaseStorageHelper(context: Context) {

    fun restartUploadTask() {


    }

    fun getItemStorageReference(itemId: String, trackerId: String, userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child("entry_data").child(userId).child(trackerId).child(itemId)
    }

    fun getDownloadUrl(pathString: String): Single<Uri> {
        return Single.create {
            subscriber ->
            println("getting download url from firebase : ${pathString}")
            val urlTask = FirebaseStorage.getInstance().reference.child(pathString).downloadUrl
            urlTask.addOnCompleteListener(object : OnCompleteListener<Uri> {
                override fun onComplete(task: Task<Uri>) {
                    if (task.isSuccessful) {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onSuccess(task.result)
                        }
                    } else {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(task.exception)
                        }
                    }
                }

            }).addOnSuccessListener {
                uri ->
                subscriber.onSuccess(uri)
            }.addOnFailureListener {
                error ->
                error.printStackTrace()
                println(error)
            }
        }
    }

    fun downloadFileAsStream(pathString: String): Single<InputStream> {
        return Single.create {
            subscriber ->

            val downloadTask = FirebaseStorage.getInstance().reference.child(pathString).stream
            val listener = object : OnCompleteListener<StreamDownloadTask.TaskSnapshot> {
                override fun onComplete(task: Task<StreamDownloadTask.TaskSnapshot>) {
                    if (task.isSuccessful) {
                        if (!subscriber.isUnsubscribed) {
                            val inputStream = task.result.stream
                            subscriber.onSuccess(inputStream)
                        }
                    } else {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(task.exception)
                        }
                    }
                }

            }

            downloadTask.addOnCompleteListener(listener)


            subscriber.add(Subscriptions.create {
                if (downloadTask.isInProgress) {
                    downloadTask.cancel()
                }
            })

        }
    }

    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return Single.create {
            subscriber ->

            println("download firebase from ${pathString} to ${localUri}")

            val localFileRoot = File(localUri.path).parentFile
            if (!localFileRoot.exists()) {
                localFileRoot.mkdirs()
            }

            val downloadTask = FirebaseStorage.getInstance().reference.child(pathString).getFile(localUri)
            val listener = object : OnCompleteListener<FileDownloadTask.TaskSnapshot> {
                override fun onComplete(task: Task<FileDownloadTask.TaskSnapshot>) {
                    if (!subscriber.isUnsubscribed) {
                        if (task.isSuccessful) {
                            println("firebase storage image successfully downloaded at ${localUri}")
                            subscriber.onSuccess(localUri)
                        } else {
                            task.exception?.printStackTrace()
                            subscriber.onError(task.exception)
                        }
                    }
                }

            }

            downloadTask.addOnCompleteListener(listener)

            subscriber.add(
                    Subscriptions.create {
                        println("download task was unsubscribed.")
                        if (downloadTask.isInProgress)
                            downloadTask.cancel()

                        downloadTask.removeOnCompleteListener(listener)
                    }
            )
        }

    }

}