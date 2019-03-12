package kr.ac.snu.hcil.android.common.serialization

/**
 * Created by younghokim on 16. 7. 21..
 */
interface IStringSerializable {

    fun fromSerializedString(serialized: String): Boolean
    fun getSerializedString(): String
}