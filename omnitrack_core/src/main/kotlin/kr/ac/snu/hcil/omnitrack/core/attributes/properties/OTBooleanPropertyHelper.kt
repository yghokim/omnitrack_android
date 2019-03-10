package kr.ac.snu.hcil.omnitrack.core.attributes.properties

/**
 * Created by younghokim on 16. 8. 12..
 */
class OTBooleanPropertyHelper : OTPropertyHelper<Boolean>() {
    override fun getSerializedValue(value: Boolean): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Boolean {
        return serialized.toBoolean()
    }

}