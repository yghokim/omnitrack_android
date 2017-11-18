package kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels

import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper

/**
 * Created by Young-Ho on 11/16/2017.
 */
open class LocalMediaCacheEntry: RealmObject(){
    @PrimaryKey
    var id: String = ""

    var originalMimeType: String = "*/*"
    var originalFileByteSize: Long = 0
    var originalFileName: String = ""

    @Index
    var serverPath: String = ""

    @Index
    var localUri: String = ""

    @Index
    var synchronizedAt: Long? = null


    fun localUriCompat(): Uri {
        return FileHelper.getSchemedUri(localUri)
    }
}