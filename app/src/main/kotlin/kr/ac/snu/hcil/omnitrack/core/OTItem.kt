package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.IDatabaseStorable
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.stringKeyEntryParser
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 22
 */
class OTItem : ADataRow, IDatabaseStorable {

    enum class LoggingSource(val nameResId: Int) {
        Unspecified(R.string.msg_tracking_source_unspecified),
        Trigger(R.string.msg_tracking_source_trigger),
        Shortcut(R.string.msg_tracking_source_shortcut),
        Manual(R.string.msg_tracking_source_manual);

        val sourceText: String by lazy {
            OTApplication.app.resources.getString(nameResId)
        }
    }

    companion object {
        inline fun <reified T> extractNotNullValues(items: Collection<OTItem>, attribute: OTAttribute<out Any>, out: MutableList<T>): Int {
            var count = 0

            for (item in items) {
                if (item.hasValueOf(attribute)) {
                    val value = item.getValueOf(attribute)
                    if (value is T) {
                        out.add(value)
                        count++
                    }
                }
            }
            return count
        }

        fun createItemsWithColumnArrays(tracker: OTTracker, timestamps: LongArray, source: LoggingSource, outItems: MutableList<OTItem>, vararg columnValuesArray: Array<Any?>) {

            if (columnValuesArray.size > 0) {
                val numItems = columnValuesArray[0].size

                for (columnValues in columnValuesArray) {
                    if (numItems != columnValues.size) {
                        throw IllegalArgumentException("The size of column values are different.")
                    }
                }

                for (i in 0..numItems - 1) {
                    val values = Array(columnValuesArray.size) {
                        index ->
                        columnValuesArray[index][i]
                    }

                    outItems.add(
                            OTItem(
                                    tracker,
                                    timestamps[i],
                                    source,
                                    *values
                            )
                    )
                }
            }
        }
    }

    val trackerObjectId: String

    override var objectId: String?
        set(value) {
            if (field != null) {
                throw Exception("objectId already assigned.")
            } else {
                field = value
            }
        }

    var timestamp: Long = -1
        private set

    val timestampString: String get() = "${TimeHelper.FORMAT_DATETIME.format(Date(timestamp))} | $timestamp"

    var source: LoggingSource
        private set

    constructor(trackerObjectId: String, source: LoggingSource) : super() {
        objectId = null
        this.trackerObjectId = trackerObjectId
        this.source = source
    }

    constructor(objectId: String, trackerObjectId: String, serializedValueTable: Map<String, String>?, timestamp: Long, source: LoggingSource) {
        this.objectId = objectId
        this.trackerObjectId = trackerObjectId
        this.timestamp = timestamp
        this.source = source

        if (serializedValueTable != null) {
            for ((key, value) in serializedValueTable) {
                valueTable[key] = TypeStringSerializationHelper.deserialize(value)
            }
        }
    }

    /**
     * used to log directly in code behind
     */
    constructor(tracker: OTTracker, timestamp: Long, source: LoggingSource, vararg values: Any?) : this(tracker.objectId, source)
    {
        this.timestamp = timestamp
        this.source = source

        if(tracker.attributes.size != values.size)
        {
            throw IllegalArgumentException("attribute count and value count is different. - attribute count is ${tracker.attributes.size}, input value count is ${values.size}")
        }

        for(valueEntry in values.withIndex())
        {
            val value = valueEntry.value
            if (value != null)
            {
                setValueOf(tracker.attributes[valueEntry.index], value)
            }
        }
    }

    fun overwriteWithPojo(pojo: FirebaseDbHelper.ItemPOJO) {
        this.timestamp = pojo.getTimestamp()
        this.source = OTItem.LoggingSource.values()[pojo.sourceType]
        valueTable.clear()
        pojo.dataTable?.let {
            table ->
            for ((key, value) in table) {
                valueTable[key] = TypeStringSerializationHelper.deserialize(value)
            }
        }
    }

    fun getSerializedValueTable(scheme: OTTracker): String {
        return stringKeyEntryParser.toJson(tableToSerializedEntryArray(scheme))
    }

    override fun extractFormattedStringArray(scheme: OTTracker): Array<String?> {
        return scheme.attributes.unObservedList.map {
            val value = getCastedValueOf(it)
            if (value != null) {
                it.formatAttributeValue(value).toString()
            } else {
                null
            }
        }.toTypedArray()
    }

    override fun extractValueArray(scheme: OTTracker): Array<Any?> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return "OTItem ${super.toString()}"
    }
}