package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
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

        const val BIN_SIZE_HOUR = 0
        const val BIN_SIZE_DAY = 1
    }

    var mode: Int = TYPE_PIVOT_TIMESTAMP

    /*** not used in LINK_TIMESPAN mode.
     *
     */
    var binSize: Int = BIN_SIZE_DAY

    /*** not used in LINK_TIMESPAN mode.
     *
     */
    var binOffset: Int = -1

    var linkedAttribute: OTTimeSpanAttribute? = null

    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        typedQueue.putInt(mode)
        if (mode == TYPE_PIVOT_TIMESTAMP || mode == TYPE_PIVOT_KEY_TIME) {
            typedQueue.putInt(binSize)
            typedQueue.putInt(binOffset)
        } else if (mode == TYPE_LINK_TIMESPAN) {
            if (linkedAttribute != null) {
                typedQueue.putString(linkedAttribute!!.objectId)
            }
        }
    }

    override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        mode = typedQueue.getInt()
        if (mode == TYPE_PIVOT_TIMESTAMP || mode == TYPE_PIVOT_KEY_TIME) {
            binSize = typedQueue.getInt()
            binOffset = typedQueue.getInt()
        } else if (mode == TYPE_LINK_TIMESPAN) {
            val attrId = typedQueue.getString()
            linkedAttribute = OmniTrackApplication.app.currentUser.findAttributeByObjectId(attrId) as OTTimeSpanAttribute
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
            }

            start = cal.timeInMillis

            when (binSize) {
                BIN_SIZE_DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                BIN_SIZE_HOUR -> cal.add(Calendar.HOUR_OF_DAY, 1)
            }

            end = cal.timeInMillis

            println("Range to be queried: $start ~ $end")
            return Pair(start, end)
        } else {
            //TODO implement other types
            throw NotImplementedError()
        }
    }

}