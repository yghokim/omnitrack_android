package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import java.math.BigDecimal

/**
 * Created by Young-Ho Kim on 2016-07-27.
 */
object TypeStringSerializationHelper {
    data class ParcelWithType(var t: String, var v: String)

    const val TYPENAME_INT = "I"
    const val TYPENAME_LONG = "L"
    const val TYPENAME_BIGDECIMAL = "D"
    const val TYPENAME_TIMEPOINT = "T"
    const val TYPENAME_STRING = "S"

    val classNameDictionary = mapOf(
            Int::class.java.name to TYPENAME_INT,
            Long::class.java.name to TYPENAME_LONG,
            BigDecimal::class.java.name to TYPENAME_BIGDECIMAL,
            TimePoint::class.java.name to TYPENAME_TIMEPOINT,
            String::class.java.name to TYPENAME_STRING
    )

    private val parcelCache = ParcelWithType("", "")
    private val gson = Gson()

    fun serialize(typeName: String, value: Any): String {
        parcelCache.t = typeName
        parcelCache.v = when (parcelCache.t) {
            TYPENAME_BIGDECIMAL -> (value as BigDecimal).toPlainString()
            TYPENAME_TIMEPOINT -> (value as TimePoint).getSerializedString()
            else -> value.toString()
        }

        return gson.toJson(parcelCache)
    }

    fun serialize(value: Any): String {
        if(classNameDictionary.containsKey(value.javaClass.name))
        {
            return serialize(classNameDictionary[value.javaClass.name]!!, value)
        }
        else throw Exception("Serialization of this type [${value.javaClass.name}] is not implemented.")
    }

    fun deserialize(serialized: String): Any {
        val parcel = gson.fromJson(serialized, ParcelWithType::class.java)
        return when (parcel.t) {
            TYPENAME_INT -> return parcel.v.toInt()
            TYPENAME_LONG -> return parcel.v.toLong()
            TYPENAME_STRING -> return parcel.v
            TYPENAME_BIGDECIMAL -> return BigDecimal(parcel.v)
            TYPENAME_TIMEPOINT -> return TimePoint(parcel.v)
            else -> return parcel.v
        }
    }

}