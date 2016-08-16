package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.database.IDatabaseStorable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 7. 22..
 */
class OTItem : ADataRow, IDatabaseStorable {

    val trackerObjectId: String

    override var dbId: Long?
        set(value) {
            if (field != null) {
                throw Exception("dbId already assigned.")
            } else {
                field = value
            }
        }

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

    fun getSerializedValueTable(scheme: OTTracker): String {
        return Gson().toJson(tableToSerializedEntryArray(scheme))
    }

    override fun extractFormattedStringArray(scheme: OTTracker): Array<String?> {
        return scheme.attributes.unObservedList.map {
            val value = getCastedValueOf(it)
            if (value != null) {
                it.formatAttributeValue(value)
            } else {
                null
            }
        }.toTypedArray()
    }

    override fun extractValueArray(scheme: OTTracker): Array<Any?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return "Item for [${OmniTrackApplication.app.currentUser[trackerObjectId]?.name}] ${super.toString()}"
    }
}