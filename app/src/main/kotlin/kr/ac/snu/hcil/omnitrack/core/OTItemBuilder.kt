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

    internal data class Parcel(val trackerObjectId: String, val mode: Int, val connectedItemDbId: Long, val valueTable: Array<String>)

    private lateinit var tracker: OTTracker

    private val connectedItemDbId: Long

    private val mode: Int


    /**
     * Used when editing item.
     * @param item: item should be already stored in DB. (Every item is immediately stored in DB when created.)
     */
    constructor(item: OTItem, tracker: OTTracker) {
        this.tracker = tracker
        this.mode = MODE_EDIT
        connectedItemDbId = item.dbId!!
        syncFromTrackerScheme()

        for (attribute in tracker.attributes) {
            setValueOf(attribute, item.getValueOf(attribute)!!)
        }
    }


    /**
     * Used when new item input mode
     */
    constructor(tracker: OTTracker, mode: Int) {
        this.tracker = tracker
        this.mode = mode
        connectedItemDbId = -1
        syncFromTrackerScheme()

        if (mode == MODE_BACKGROUND)
            autoCompleteAsync()
    }

    @SuppressWarnings("NotUsed")
    constructor(trackerId: String, mode: Int) {
        this.mode = mode
        connectedItemDbId = -1
        reloadTracker(trackerId)
    }

    /**
     * used when deserializing
     */
    constructor(serialized: String) {

        val parser = Gson()
        val parcel = parser.fromJson(serialized, Parcel::class.java)

        this.mode = parcel.mode
        this.connectedItemDbId = parcel.connectedItemDbId

        reloadTracker(parcel.trackerObjectId)

        val s = parcel.valueTable.map { parser.fromJson(it, SerializedStringKeyEntry::class.java) }

        for (entry in s) {
            valueTable[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
        }
        syncFromTrackerScheme()
    }

    fun autoCompleteAsync(finished: (() -> Unit)? = null) {

        var remain = tracker.attributes.size
        if (remain == 0) {
            finished?.invoke()
            return
        }

            for (attribute in tracker.attributes) {
                if (attribute.valueConnection == null) {
                    attribute.getAutoCompleteValueAsync {
                        result ->
                        remain--
                        setValueOf(attribute, result)

                        if (remain == 0) {
                            //finish
                            finished?.invoke()
                            println("finished autocompleting builder")
                        }
                    }
                } else {
                    println("request value connection")
                    attribute.valueConnection?.requestValueAsync(this) {
                        value: Any? ->
                        if (value != null) {
                            remain--
                            setValueOf(attribute, value)
                            if (remain == 0) {
                                //finish
                                finished?.invoke()
                                println("finished autocompleting builder")
                            }
                        } else {
                            attribute.getAutoCompleteValueAsync {
                                result ->
                                remain--
                                setValueOf(attribute, result)

                                if (remain == 0) {
                                    //finish
                                    finished?.invoke()
                                    println("finished autocompleting builder")
                                }
                            }
                        }
                    }
                }
            }
    }

    fun reloadTracker(trackerObjectId: String) {
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
        return parser.toJson(Parcel(tracker.objectId, mode, connectedItemDbId, tableToSerializedEntryArray(tracker)))
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