package kr.ac.snu.hcil.omnitrack.utils.serialization

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
abstract class ATypedQueueSerializable() : IStringSerializable {

    constructor(serialized: String) : this() {
        fromSerializedString(serialized)
    }

    override fun fromSerializedString(serialized: String): Boolean {
        onDeserialize(SerializableTypedQueue(serialized))
        return true
    }

    override fun getSerializedString(): String {
        val typedQueue = SerializableTypedQueue()
        onSerialize(typedQueue)
        return typedQueue.getSerializedString()
    }

    abstract fun onSerialize(typedQueue: SerializableTypedQueue)

    abstract fun onDeserialize(typedQueue: SerializableTypedQueue)
}