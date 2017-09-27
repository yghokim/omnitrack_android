package kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos

import android.support.annotation.Keep
import kr.ac.snu.hcil.omnitrack.core.OTItem

/**
 * Created by younghokim on 2017. 9. 27..
 */
@Keep
class ItemPOJO {
    var objectId: String? = null

    var trackerObjectId: String = ""

    var deviceId: String? = null

    var timestamp: Long = 0

    var source: String? = null

    var serializedValueTable: Map<String, String>? = null

    var updatedAt: Long = System.currentTimeMillis()

    var loggingSource: String = OTItem.LoggingSource.Unspecified.name
}