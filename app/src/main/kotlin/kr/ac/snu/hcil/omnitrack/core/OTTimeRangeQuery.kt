package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTTimeRangeQuery : ATypedQueueSerializable() {


    interface ITimeMethod {
        fun getStartTime(): Long
        fun getEndTime(): Long
    }

    class ABinTimeMethod {

    }


    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}