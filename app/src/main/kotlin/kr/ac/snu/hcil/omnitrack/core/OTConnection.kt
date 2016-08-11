package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection : ATypedQueueSerializable() {

    companion object {

    }

    var source: OTMeasureFactory.OTMeasure? = null
        get() {
            return field
        }
        set(value) {
            if (field != value) {
                field = value
            }
        }

    val isTimeQueryAvailable: Boolean
        get() = if (source != null) {
            source?.factory?.isRangedQueryAvailable ?: false
        } else false


    override fun onSerialize(typedQueue: SerializableTypedQueue) {

    }

    override fun onDeserialize(typedQueue: SerializableTypedQueue) {

    }

}
