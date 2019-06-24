package kr.ac.snu.hcil.omnitrack.core.fields.properties

/**
 * Created by younghokim on 16. 7. 12..
 */
class OTSelectionPropertyHelper : OTPropertyHelper<Int>() {
    override fun getSerializedValue(value: Int): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Int {
        return serialized.toInt()
    }

}