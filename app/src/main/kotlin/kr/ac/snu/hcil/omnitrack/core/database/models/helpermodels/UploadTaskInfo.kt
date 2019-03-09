package kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels

import android.net.Uri
import io.realm.MutableRealmInteger
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.file.FileHelper

/**
 * Created by Young-Ho on 3/4/2017.
 */
open class UploadTaskInfo : RealmObject() {
    @PrimaryKey
    var id: String = ""

    var sessionUri: String? = null

    var serverPath: String = ""
    var localFilePath: String = ""
    var localFileMimeType: String = "*/*"

    val trialCount = MutableRealmInteger.valueOf(0)

    fun localUriCompat(): Uri {
        return FileHelper.getSchemedUri(localFilePath)
    }
}