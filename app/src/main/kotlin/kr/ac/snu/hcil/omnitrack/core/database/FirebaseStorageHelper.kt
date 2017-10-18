package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StreamDownloadTask
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import java.io.File
import java.io.InputStream


/**
 * Created by Young-Ho on 3/4/2017.
 */
class FirebaseStorageHelper(context: Context) {

    fun restartUploadTask() {


    }

    fun getDownloadUrl(pathString: String): Single<Uri> {
        return Single.create {
            subscriber ->
            println("getting download url from firebase : ${pathString}")
            val urlTask = FirebaseStorage.getInstance().reference.child(pathString).downloadUrl
            urlTask.addOnCompleteListener(object : OnCompleteListener<Uri> {
                override fun onComplete(task: Task<Uri>) {
                    if (task.isSuccessful) {
                        if (!subscriber.isDisposed) {
                            subscriber.onSuccess(task.result)
                        }
                    } else {
                        if (!subscriber.isDisposed) {
                            subscriber.onError(task.exception ?: Exception("getDownloadUrl task was failed."))
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
                        if (!subscriber.isDisposed) {
                            val inputStream = task.result.stream
                            subscriber.onSuccess(inputStream)
                        }
                    } else {
                        if (!subscriber.isDisposed) {
                            subscriber.onError(task.exception ?: Error("DownloadFileToStream task was failed."))
                        }
                    }
                }

            }

            downloadTask.addOnCompleteListener(listener)


            subscriber.setDisposable(Disposables.fromAction {
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
                    if (!subscriber.isDisposed) {
                        if (task.isSuccessful) {
                            println("firebase storage image successfully downloaded at ${localUri}")
                            subscriber.onSuccess(localUri)
                        } else {
                            task.exception?.printStackTrace()
                            subscriber.onError(task.exception ?: Exception("downloadfile to file task was failed."))
                        }
                    }
                }

            }

            downloadTask.addOnCompleteListener(listener)

            subscriber.setDisposable(
                    Disposables.fromAction {
                        println("download task was unsubscribed.")
                        if (downloadTask.isInProgress)
                            downloadTask.cancel()
                        downloadTask.removeOnCompleteListener(listener)
                    }
            )
        }

    }

}