package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.gson.Gson
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class SerializableTypedQueue() : IStringSerializable {
    private val serializedQueue = ArrayDeque<String>()

    constructor(serialized: String) : this() {
        fromSerializedString(serialized)
    }

    override fun fromSerializedString(serialized: String): Boolean {
        serializedQueue.clear()
        val gson = Gson()
        for (entry in gson.fromJson(serialized, Array<String>::class.java)) {
            serializedQueue.push(entry)
        }

        return true
    }

    override fun getSerializedString(): String {
        return Gson().toJson(serializedQueue.toTypedArray())
    }

    fun putValue(typeName: String, value: Any) {
        serializedQueue.push(TypeStringSerializationHelper.serialize(typeName, value))
    }

    fun getValue(typeName: String): Any {
        return TypeStringSerializationHelper.deserialize(serializedQueue.poll())
    }
}