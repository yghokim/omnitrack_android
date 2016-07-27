package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.MapSerializer
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by younghokim on 16. 7. 22..
 */
abstract class ADataRow() {

    abstract fun getValueOf(attribute: OTAttribute<out Any>): Any?
    abstract fun <T> getCastedValueOf(attribute: OTAttribute<T>): T?
    abstract fun hasValueOf(attribute: OTAttribute<out Any>): Boolean
    abstract fun getNumColumns(): Int

    abstract fun extractFormattedStringArray(scheme: OTTracker): Array<String?>

    abstract fun extractValueArray(scheme: OTTracker): Array<Any?>

    protected fun tableToSerializedEntryArray(table: Map<String, out Any>, scheme: OTTracker): Array<String> {
        val s = ArrayList<String>()
        val parser = Gson()

        for (attribute in scheme.attributes) {
            if (table[attribute.objectId] != null) {
                s.add(parser.toJson(SerializedStringKeyEntry(attribute.objectId, TypeStringSerializationHelper.serialize(attribute.typeNameForSerialization, table[attribute.objectId]!!))))
            }
        }

        return s.toTypedArray()
    }
}