package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.internal.Factory
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.services.OTBinaryUploadWorker
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import java.util.*
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 15..
 */
class OTBinaryStorageController(
        val uploadRequest: Provider<OneTimeWorkRequest>,
        val core: IBinaryStorageCore, val realmProvider: Factory<Realm>) {


    fun registerNewUploadTask(localPath: String, serverFile: OTServerFile) {
        realmProvider.get().use { realm ->
            realm.executeTransactionIfNotIn {
                val dbObject = realm.where(UploadTaskInfo::class.java)
                        .equalTo("serverPath", serverFile.serverPath).findFirst() ?: realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
                dbObject.localFilePath = localPath
                dbObject.serverPath = serverFile.serverPath
                dbObject.localFileMimeType = serverFile.mimeType

                if (dbObject.trialCount.isNull) {
                    dbObject.trialCount.set(0L)
                }
            }
        }
        WorkManager.getInstance().enqueueUniqueWork(OTBinaryUploadWorker.TAG, ExistingWorkPolicy.KEEP, uploadRequest.get())
    }

    fun makeServerPath(userId: String, trackerId: String, itemId: String, attributeLocalId: String, fileIdentifier: String): String {
        return core.makeServerPath(userId, trackerId, itemId, attributeLocalId, fileIdentifier)
    }

    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return core.downloadFileTo(pathString, localUri)
    }
}