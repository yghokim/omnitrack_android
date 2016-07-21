package kr.ac.snu.hcil.omnitrack.utils.serialization

/**
 * Created by younghokim on 16. 7. 21..
 */
interface IStringSerializable {

    fun fromSerializedString(serialized: String): Boolean
    fun getSerializedString(): String
}