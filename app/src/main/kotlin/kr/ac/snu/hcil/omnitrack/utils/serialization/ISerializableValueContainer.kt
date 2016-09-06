package kr.ac.snu.hcil.omnitrack.utils.serialization

/**
 * Created by younghokim on 16. 7. 22..
 */
interface ISerializableValueContainer {
    fun setValueFromSerializedString(serialized: String): Boolean
    fun getSerializedValue(): String
}