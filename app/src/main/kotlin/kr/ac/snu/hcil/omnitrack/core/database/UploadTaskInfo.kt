package kr.ac.snu.hcil.omnitrack.core.database

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 3/4/2017.
 */
open class UploadTaskInfo : RealmObject() {
    @PrimaryKey
    var id: String = ""

    var sessionUri: String? = null

    var localUri: String = ""
    var serverUri: String = ""

    var trackerId: String = ""
    var itemId: String = ""
    var userId: String = ""
}