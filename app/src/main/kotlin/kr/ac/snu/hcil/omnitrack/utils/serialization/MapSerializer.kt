package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.gson.Gson
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-27.
 */
object MapSerializer {

    interface SerializationHelper<T> {
        fun serialize(key: T, value: Any): String
        fun deserialize(key: T, serialized: String): Any
    }

    fun <T> serializeMap(map: Map<T, out Any>, helper: SerializationHelper<T>): String {
        val list = ArrayList<String>()

        val builder = Gson()

        for (pair in map) {
            list.add(builder.toJson(SerializedKeyEntry<T>(pair.key, helper.serialize(pair.key, pair.value))))
        }

        return builder.toJson(list.toTypedArray())
    }

    fun deserializeMapWithStringKey(serialized: String, map: Hashtable<String, Any>?, helper: SerializationHelper<String>): Map<String, Any> {
        val builder = Gson()
        val entries = Gson().fromJson(serialized, Array<String>::class.java).map { builder.fromJson(it, SerializedStringKeyEntry::class.java) }

        val mapToUpdate = map ?: Hashtable<String, Any>()
        for (entry in entries) {
            mapToUpdate[entry.key] = helper.deserialize(entry.key, entry.value)
        }

        return mapToUpdate
    }

}