package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 22
 */
abstract class ADataRow {


    protected val valueTable = Hashtable<String, Any>()

    val isEmpty: Boolean
        get() = valueTable.isEmpty

    fun getValueOf(attribute: OTAttribute<out Any>): Any? {
        return valueTable[attribute.objectId]
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCastedValueOf(attribute: OTAttribute<T>): T? {
        return valueTable[attribute.objectId] as? T
    }

    fun setValueOf(attribute: OTAttribute<out Any>, value: Any) {
        valueTable[attribute.objectId] = value
    }

    fun removeValueOf(attribute: OTAttribute<out Any>) {
        valueTable.remove(attribute.objectId)
    }

    fun hasValueOf(attribute: OTAttribute<out Any>): Boolean {
        return valueTable.containsKey(attribute.objectId)
    }

    fun getNumStoredAttributes(): Int {
        return valueTable.keys.size
    }

    fun clear() {
        valueTable.clear()
    }

    abstract fun extractFormattedStringArray(scheme: OTTracker): Array<String?>

    abstract fun extractValueArray(scheme: OTTracker): Array<Any?>

    protected fun tableToSerializedEntryArray(scheme: OTTracker): Array<SerializedStringKeyEntry> {
        val s = ArrayList<SerializedStringKeyEntry>()

        for (attribute in scheme.attributes) {
            if (valueTable[attribute.objectId] != null) {
                s.add(SerializedStringKeyEntry(attribute.objectId, TypeStringSerializationHelper.serialize(attribute.typeNameForSerialization, valueTable[attribute.objectId]!!)))
            }
        }

        return s.toTypedArray()
    }

    override fun toString(): String = valueTable.map { "${it.key}: ${it.value}" }.joinToString(", ")

    fun getEntryIterator(): Iterable<Map.Entry<String, Any>> = valueTable.asIterable()
}