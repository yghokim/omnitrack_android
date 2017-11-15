package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.services.OTBinaryUploadService

/**
 * Created by younghokim on 2017. 11. 15..
 */
class OTBinaryStorageController(val context: Context, val core: IBinaryStorageCore) {
    fun makeUploadServiceIntent(outUri: SynchronizedUri, itemId: String, trackerId: String, userId: String): Intent {
        return Intent(context, OTBinaryUploadService::class.java)
                .setAction(OTBinaryUploadService.ACTION_UPLOAD)
                .putExtra(OTBinaryUploadService.EXTRA_OUT_URI, SynchronizedUri.parser.toJson(outUri))
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId)
    }


    fun makeResumeUploadIntent(): Intent {
        return Intent(context, OTBinaryUploadService::class.java)
                .setAction(OTBinaryUploadService.ACTION_RESUME)
    }

    fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri {
        return core.makeFilePath(itemId, trackerId, userId, fileName)
    }

    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return core.downloadFileTo(pathString, localUri)
    }
}