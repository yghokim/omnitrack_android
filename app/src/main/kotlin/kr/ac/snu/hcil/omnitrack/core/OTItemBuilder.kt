package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 7. 25..
 */
class OTItemBuilder : ADataRow, IStringSerializable {
    companion object {
        const val MODE_EDIT = 2
        const val MODE_FOREGROUND = 1
        const val MODE_BACKGROUND = 0
    }

    internal data class Parcel(val trackerObjectId: String, val mode: Int, val valueTable: Array<String>)

    val trackerObjectId: String
    private lateinit var tracker: OTTracker


    private var connectedItemDbId: Long = -1
    private var connectedItem: OTItem? = null


    private val mode: Int

    constructor(tracker: OTTracker, mode: Int) {
        this.trackerObjectId = tracker.objectId
        this.tracker = tracker
        this.mode = mode
        //reloadTracker()
        syncFromTrackerScheme()

        if (mode == MODE_BACKGROUND)
            autoComplete()
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
            valueTable[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
        }
        syncFromTrackerScheme()
    }

    fun autoComplete() {
        for (attribute in tracker.attributes) {
            setValueOf(attribute, attribute.makeDefaultValue())
        }
    }

    fun reloadTracker() {
        tracker = OmniTrackApplication.app.currentUser[trackerObjectId]!!
        syncFromTrackerScheme()
    }

    fun syncFromTrackerScheme() {

        val it = valueTable.entries.iterator()

        while (it.hasNext()) {
            val entry = it.next()

            if (tracker.attributes.unObservedList.find { it.objectId == entry.key } == null) {
                it.remove()
            }
        }
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
        val parser = Gson()
        return parser.toJson(Parcel(trackerObjectId, mode, tableToSerializedEntryArray(tracker)))
    }

    fun makeItem(): OTItem {
        val item = OTItem(tracker.objectId)

        for (attribute in tracker.attributes) {
            if (hasValueOf(attribute)) {
                item.setValueOf(attribute, getValueOf(attribute)!!)
            }
        }

        return item
    }
}