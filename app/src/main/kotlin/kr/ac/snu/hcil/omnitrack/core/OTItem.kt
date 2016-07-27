package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.MapSerializer
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by younghokim on 16. 7. 22..
 */
class OTItem : ADataRow {

    private val trackerObjectId: String

    val dbId: Long?

    var timestamp: Long = -1
        private set

    constructor(trackerObjectId: String) : super() {
        dbId = null
        this.trackerObjectId = trackerObjectId
    }

    constructor(dbId: Long, trackerObjectId: String, serializedValues: String, timestamp: Long) {
        this.dbId = dbId
        this.trackerObjectId = trackerObjectId
        this.timestamp = timestamp

        val parser = Gson()
        val s = parser.fromJson(serializedValues, Array<String>::class.java).map { parser.fromJson(it, SerializedStringKeyEntry::class.java) }
        for (entry in s) {
            valueTable[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
        }
    }

    override fun extractFormattedStringArray(scheme: OTTracker): Array<String?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun extractValueArray(scheme: OTTracker): Array<Any?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return "Item for [${OmniTrackApplication.app.currentUser[trackerObjectId]?.name}] ${super.toString()}"
    }
}