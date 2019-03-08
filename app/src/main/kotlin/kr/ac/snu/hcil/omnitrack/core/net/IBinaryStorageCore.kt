package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UploadTaskInfo

/**
 * Created by younghokim on 2017. 11. 15..
 */
interface IBinaryStorageCore {
    fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable
    fun makeServerPath(userId: String, trackerId: String, itemId: String, attributeLocalId: String, fileIdentifier: String): String
    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri>
    fun decodeTrackerIdFromServerPath(serverPath: String): String?
}