package kr.ac.snu.hcil.omnitrack.core.net

import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo

/**
 * Created by younghokim on 2017. 11. 15..
 */
interface IBinaryStorageCore {
    fun startNewUploadTaskImpl(taskInfo: UploadTaskInfo, onProgress: (session: String) -> Unit): Completable
    fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri
    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri>
}