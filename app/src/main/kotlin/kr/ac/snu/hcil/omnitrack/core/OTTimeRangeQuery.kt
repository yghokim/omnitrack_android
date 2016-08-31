package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeSpanAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTTimeRangeQuery : ATypedQueueSerializable() {

    companion object {
        const val TYPE_PIVOT_TIMESTAMP = 0
        const val TYPE_PIVOT_KEY_TIME = 1
        const val TYPE_LINK_TIMESPAN = 2
        const val TYPE_PIVOT_TIMEPOINT = 3


        const val BIN_SIZE_HOUR = 0
        const val BIN_SIZE_DAY = 1
        const val BIN_SIZE_WEEK = 2
    }

    var mode: Int = TYPE_PIVOT_TIMESTAMP

    val isBinAndOffsetAvailable: Boolean
        get() = mode == TYPE_PIVOT_TIMEPOINT || mode == TYPE_PIVOT_TIMESTAMP || mode == TYPE_PIVOT_KEY_TIME

    val needsLinkedAttribute: Boolean
        get() = mode == TYPE_PIVOT_TIMEPOINT || mode == TYPE_LINK_TIMESPAN

    /*** not used in LINK_TIMESPAN mode.
     *
     */
    var binSize: Int = BIN_SIZE_DAY

    /*** not used in LINK_TIMESPAN mode.
     *
     */
    var binOffset: Int = 0

    var linkedAttribute: OTAttribute<out Any>? = null

    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        typedQueue.putInt(mode)
        if (isBinAndOffsetAvailable) {
            typedQueue.putInt(binSize)
            typedQueue.putInt(binOffset)
        }

        if (needsLinkedAttribute) {
            if (linkedAttribute != null) {
                typedQueue.putString(linkedAttribute!!.objectId)
            }
        }
    }

    override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        mode = typedQueue.getInt()
        if (isBinAndOffsetAvailable) {
            binSize = typedQueue.getInt()
            binOffset = typedQueue.getInt()
        }

        if (needsLinkedAttribute) {
            val attrId = typedQueue.getString()
            linkedAttribute = OTApplication.app.currentUser.findAttributeByObjectId(attrId) as OTTimeSpanAttribute
        }
    }


    fun getRange(builder: OTItemBuilder): Pair<Long, Long> {
        var start: Long
        var end: Long
        if (mode == TYPE_PIVOT_TIMESTAMP) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, 0)
            if (binSize == BIN_SIZE_DAY) {
                cal.set(Calendar.HOUR_OF_DAY, 0)
            }

            when (binSize) {
                BIN_SIZE_DAY -> cal.add(Calendar.DAY_OF_YEAR, binOffset)
                BIN_SIZE_HOUR -> cal.add(Calendar.HOUR_OF_DAY, binOffset)
                BIN_SIZE_WEEK -> cal.add(Calendar.WEEK_OF_YEAR, binOffset)
            }

            start = cal.timeInMillis

            when (binSize) {
                BIN_SIZE_DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                BIN_SIZE_HOUR -> cal.add(Calendar.HOUR_OF_DAY, 1)
                BIN_SIZE_WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            }

            end = cal.timeInMillis

            println("Range to be queried: $start ~ $end")
            return Pair(start, end)
        } else {
            //TODO implement other types
            throw NotImplementedError()
        }
    }

    fun getModeName(context: Context): CharSequence {
        if (mode == OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP) {
            return context.resources.getString(R.string.msg_connection_wizard_time_query_pivot_present)
        } else if (mode == OTTimeRangeQuery.TYPE_PIVOT_TIMEPOINT || mode == OTTimeRangeQuery.TYPE_LINK_TIMESPAN) {
            val fieldNameFormat = context.resources.getString(R.string.msg_connection_wizard_time_query_pivot_field_format)
            return String.Companion.format(fieldNameFormat, linkedAttribute?.name ?: "")
        } else return "None"
    }

    fun getScopeName(context: Context): CharSequence {
        return when (binSize) {
            BIN_SIZE_DAY -> context.resources.getString(R.string.msg_connection_wizard_time_query_scope_day)
            BIN_SIZE_HOUR -> context.resources.getString(R.string.msg_connection_wizard_time_query_scope_hour)
            BIN_SIZE_WEEK -> context.resources.getString(R.string.msg_connection_wizard_time_query_scope_week)
            else -> "None"
        }
    }

}