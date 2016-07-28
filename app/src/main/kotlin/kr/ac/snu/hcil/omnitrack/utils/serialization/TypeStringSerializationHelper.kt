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
    const val TYPENAME_INT_ARRAY = "I[]"
    const val TYPENAME_LONG_ARRAY = "L[]"

    val classNameDictionary = mapOf(
            Int::class.java.name to TYPENAME_INT,
            Long::class.java.name to TYPENAME_LONG,
            BigDecimal::class.java.name to TYPENAME_BIGDECIMAL,
            TimePoint::class.java.name to TYPENAME_TIMEPOINT,
            String::class.java.name to TYPENAME_STRING,
            IntArray::class.java.name to TYPENAME_INT_ARRAY,
            LongArray::class.java.name to TYPENAME_LONG_ARRAY
    )

    private val parcelCache = ParcelWithType("", "")
    private val gson = Gson()


    fun serialize(typeName: String, value: Any): String {
        parcelCache.t = typeName
        parcelCache.v = when (parcelCache.t) {
            TYPENAME_BIGDECIMAL -> (value as BigDecimal).toPlainString()
            TYPENAME_TIMEPOINT -> (value as TimePoint).getSerializedString()
            TYPENAME_INT_ARRAY -> (value as IntArray).joinToString(",")
            TYPENAME_LONG_ARRAY -> (value as IntArray).joinToString(",")

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
            TYPENAME_INT_ARRAY -> return parcel.v.split(",").map { it.toInt() }.toIntArray()
            TYPENAME_LONG_ARRAY -> return parcel.v.split(",").map { it.toLong() }.toLongArray()

            else -> return parcel.v
        }
    }

}