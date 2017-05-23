package kr.ac.snu.hcil.omnitrack.utils

import io.realm.RealmObject

/**
 * Created by Young-Ho on 5/24/2017.
 */
open class VectorIconBitmapCache : RealmObject() {
    var resourceId: Int = 0
    var sizeDp: Int? = 24
    var tint: Int? = null
    var uri: String = ""
}