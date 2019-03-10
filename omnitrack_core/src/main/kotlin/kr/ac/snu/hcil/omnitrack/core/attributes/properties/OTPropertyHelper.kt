package kr.ac.snu.hcil.omnitrack.core.attributes.properties

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTPropertyHelper<T> {

    abstract fun getSerializedValue(value: T): String

    abstract fun parseValue(serialized: String): T

}