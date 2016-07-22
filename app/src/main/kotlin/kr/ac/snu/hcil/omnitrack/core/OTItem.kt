package kr.ac.snu.hcil.omnitrack.core

import java.util.*

/**
 * Created by younghokim on 16. 7. 22..
 */
class OTItem(parent: OTTracker) : ADataRow(parent) {

    var timestamp: Long = 0
    private var data = Hashtable<Long, String>()

    override fun getValueOf(attribute: OTAttribute<out Any>): Any {
        return attribute.parseAttributeValue(data[attribute.objectId.toLong()]!!)
    }

    override fun <T> getCastedValueOf(attribute: OTAttribute<T>): T {
        return attribute.parseAttributeValue(data[attribute.objectId.toLong()]!!)
    }

    override fun hasValueOf(attribute: OTAttribute<out Any>): Boolean {
        return data.contains(attribute.objectId.toLong())
    }

    override fun getNumColumns(): Int {
        return data.keys.size
    }

    override fun extractFormattedStringArray(scheme: OTTracker): Array<String?> {
        return scheme.getTotalAttributes().map { it.formatAttributeValue(getValueOf(it)) }.toTypedArray()
    }

    override fun extractValueArray(scheme: OTTracker): Array<Any?> {
        return scheme.getTotalAttributes().map { getValueOf(it) }.toTypedArray()
    }


    override fun fromSerializedString(serialized: String): Boolean {
        return true
    }

    override fun getSerializedString(): String {
        return ""
    }
}