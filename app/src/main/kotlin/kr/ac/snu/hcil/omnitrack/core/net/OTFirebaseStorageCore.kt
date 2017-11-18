package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposables
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UploadTaskInfo
import java.io.File

/**
 * Created by younghokim on 2017. 11. 15..
 */
class OTFirebaseStorageCore : IBinaryStorageCore {
    private fun getItemStorageReference(relPath: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(relPath)
    }

    override fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable {
        return Completable.create { subscriber ->


            val localUri = taskInfo.localUriCompat()

            val storageRef = getItemStorageReference(taskInfo.serverPath).child("")

            val task = storageRef.putFile(localUri, StorageMetadata.Builder().build(), if (taskInfo.sessionUri != null) Uri.parse(taskInfo.sessionUri!!) else null)

            task.addOnProgressListener { snapshot: UploadTask.TaskSnapshot ->
                val sessionUri = snapshot.uploadSessionUri?.toString()
                println("refresh sessionUri: ${snapshot.uploadSessionUri?.toString()}")

                if (sessionUri != null) {
                    onProgress.invoke(sessionUri)
                }
            }.addOnCompleteListener { finishedTask ->
                if (finishedTask.isSuccessful) {
                    println("Firebsae Storage upload was successful: ${finishedTask.result.downloadUrl}")
                    if (!subscriber.isDisposed) {
                        subscriber.onComplete()
                    }
                } else {
                    if (!subscriber.isDisposed) {
                        finishedTask.exception?.printStackTrace()
                        subscriber.onError(finishedTask.exception!!)
                    }
                }
            }
        }
    }

    override fun makeServerPath(userId: String, trackerId: String, itemId: String, attributeLocalId: String, fileIdentifier: String): String
            = "entry_data/$userId/$trackerId/$itemId/$attributeLocalId/$fileIdentifier"

    override fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return Single.create { subscriber ->

            println("download firebase from ${pathString} to ${localUri}")

            val localFileRoot = File(localUri.path).parentFile
            if (!localFileRoot.exists()) {
                localFileRoot.mkdirs()
            }

            val downloadTask = getItemStorageReference(pathString).getFile(localUri)
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