package kr.ac.snu.hcil.omnitrack.core

import android.os.Parcelable
import com.google.gson.*
import com.google.gson.annotations.Expose
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedIntegerKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import java.lang.reflect.Type
import java.util.*

/**
 * Created by younghokim on 16. 7. 25..
 */
class OTItemBuilder : ADataRow {

    companion object {
        const val MODE_FOREGROUND = 1
        const val MODE_BACKGOUND = 0
    }

    internal data class Parcel(val trackerObjectId: String, val mode: Int, val valueTable: Array<String>)

    val trackerObjectId: String
    private lateinit var tracker: OTTracker

    private val mode: Int

    private val valueTable = Hashtable<String, Any>()

    constructor(tracker: OTTracker, mode: Int) {
        this.trackerObjectId = tracker.objectId
        this.tracker = tracker
        this.mode = mode
        //reloadTracker()
        syncFromTrackerScheme()
    }

    @SuppressWarnings("NotUsed")
    constructor(trackerId: String, mode: Int) {
        this.trackerObjectId = tracker.objectId
        this.mode = mode
        reloadTracker()
    }

    constructor(serialized: String) {

        val parser = Gson()
        val parcel = parser.fromJson(serialized, Parcel::class.java)

        this.trackerObjectId = parcel.trackerObjectId
        this.mode = parcel.mode
        reloadTracker()

        val s = parcel.valueTable.map { parser.fromJson(it, SerializedStringKeyEntry::class.java) }

        for (entry in s) {
            valueTable[entry.key] = entry.value
        }
        syncFromTrackerScheme()

        for (attribute in tracker.attributes) {
            val tableValue = valueTable[attribute.objectId]
            if (tableValue is String) {
                valueTable[attribute.objectId] = attribute.deserializeAttributeValue(tableValue)
            }
        }

        println("[Restored ItemBuilder]")
        for (key in valueTable) {
            println("key : ${key.key}, value : ${key.value}")
        }
    }

    fun reloadTracker() {
        tracker = OmniTrackApplication.app.currentUser[trackerObjectId]!!
        syncFromTrackerScheme()
    }

    fun syncFromTrackerScheme() {
        for (key in valueTable.keys) {
            if (tracker.attributes.unObservedList.find { it.objectId == key } == null) {
                valueTable.remove(key)
            }
        }
    }

    override fun getValueOf(attribute: OTAttribute<out Any>): Any? {
        return valueTable[attribute.objectId]
    }

    override fun <T> getCastedValueOf(attribute: OTAttribute<T>): T? {
        return valueTable[attribute.objectId] as? T
    }

    fun setValueOf(attribute: OTAttribute<out Any>, value: Any) {
        valueTable[attribute.objectId] = value
    }

    override fun hasValueOf(attribute: OTAttribute<out Any>): Boolean {
        return valueTable.contains(attribute.objectId)
    }

    override fun getNumColumns(): Int {
        return valueTable.keys.size
    }

    override fun extractFormattedStringArray(scheme: OTTracker): Array<String?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun extractValueArray(scheme: OTTracker): Array<Any?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fromSerializedString(serialized: String): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSerializedString(): String {

        val s = ArrayList<String>()
        val parser = Gson()

        for (attribute in tracker.attributes) {
            if (valueTable[attribute.objectId] != null) {
                s.add(parser.toJson(SerializedStringKeyEntry(attribute.objectId, attribute.serializeAttributeValue(valueTable[attribute.objectId]!!))))
            }
        }

        return parser.toJson(Parcel(trackerObjectId, mode, s.toTypedArray()))
    }
}