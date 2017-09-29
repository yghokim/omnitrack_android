package kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos

import android.support.annotation.Keep

/**
 * Created by younghokim on 2017. 9. 27..
 */
@Keep
class OTItemPOJO {
    var objectId: String = ""

    var trackerObjectId: String = ""

    var deviceId: String? = null

    var timestamp: Long = 0

    var source: String? = null

    var synchronizedAt: Long? = null

    var serializedValueTable: Map<String, String>? = null
    var removed: Boolean = false

    override fun toString(): String {
        return "objectId: ${objectId}\ntrackerId: ${trackerObjectId}\ndeviceId: ${deviceId}\ntimestamp: ${timestamp}\nsource: ${source}\nsynchronizedAt: ${synchronizedAt}\nremoved: ${removed}\nvalues: ${serializedValueTable?.size ?: "null"}"
    }
}