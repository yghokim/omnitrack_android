package kr.ac.snu.hcil.omnitrack.utils.serialization

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.datatypes.Route
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.utils.isNumericPrimitive
import kr.ac.snu.hcil.omnitrack.utils.toBigDecimal
import java.math.BigDecimal

/**
 * Created by Young-Ho Kim on 2016-07-27.
 */

fun LatLng.serialize(): String {
    return "${latitude},${longitude}"
}

fun deserializeLatLng(serialized: String): LatLng {
    val s = serialized.split(",")
    return LatLng(s[0].toDouble(), s[1].toDouble())
}

object TypeStringSerializationHelper {
    data class ParcelWithType(var t: String, var v: String)

    const val TYPENAME_INT = "I"
    const val TYPENAME_LONG = "L"
    const val TYPENAME_FLOAT = "F"
    const val TYPENAME_DOUBLE = "d"
    const val TYPENAME_BIGDECIMAL = "D"
    const val TYPENAME_TIMEPOINT = "T"
    const val TYPENAME_STRING = "S"
    const val TYPENAME_INT_ARRAY = "I[]"
    const val TYPENAME_LONG_ARRAY = "L[]"
    const val TYPENAME_LATITUDE_LONGITUDE = "Crd"
    const val TYPENAME_ROUTE = "R"
    const val TYPENAME_TIMESPAN = "TS"
    const val TYPENAME_URI = "U"



    val classNameDictionary: Map<String, String> = mapOf(
            Int::class.java.name to TYPENAME_INT,
            "java.lang.Integer" to TYPENAME_INT,
            Long::class.java.name to TYPENAME_LONG,
            "java.lang.Long" to TYPENAME_LONG,
            "java.lang.Float" to TYPENAME_FLOAT,
            "java.lang.Double" to TYPENAME_DOUBLE,
            BigDecimal::class.java.name to TYPENAME_BIGDECIMAL,
            TimePoint::class.java.name to TYPENAME_TIMEPOINT,
            TimeSpan::class.java.name to TYPENAME_TIMESPAN,
            String::class.java.name to TYPENAME_STRING,
            IntArray::class.java.name to TYPENAME_INT_ARRAY,
            LongArray::class.java.name to TYPENAME_LONG_ARRAY,
            LatLng::class.java.name to TYPENAME_LATITUDE_LONGITUDE,
            Route::class.java.name to TYPENAME_ROUTE,
            Uri::class.java.name to TYPENAME_URI
    )

    private val parcelCache = ParcelWithType("", "")
    private val gson = Gson()


    fun serialize(typeName: String, value: Any): String {
        parcelCache.t = typeName
        parcelCache.v = when (parcelCache.t) {
            TYPENAME_BIGDECIMAL -> {
                if (value is BigDecimal) {
                    value.toPlainString()
                } else {
                    if (isNumericPrimitive(value)) {
                        toBigDecimal(value).toPlainString()
                    } else throw IllegalArgumentException("input value $value is not convertible to BigDecimal.")
                }
            }
            TYPENAME_TIMEPOINT -> (value as TimePoint).getSerializedString()
            TYPENAME_TIMESPAN -> (value as TimeSpan).getSerializedString()
            TYPENAME_INT_ARRAY -> (value as IntArray).joinToString(",")
            TYPENAME_LONG_ARRAY -> (value as IntArray).joinToString(",")
            TYPENAME_LATITUDE_LONGITUDE -> (value as LatLng).serialize()
            TYPENAME_ROUTE -> (value as Route).getSerializedString()
            else -> value.toString()
        }

        return gson.toJson(parcelCache)
    }

    fun serialize(value: Any): String {
        if(classNameDictionary.containsKey(value.javaClass.name))
        {
            return serialize(classNameDictionary[value.javaClass.name]!!, value)
        } else throw Exception("Serialization of this type [${value.javaClass.name}] is not implemented, value: $value")
    }

    fun deserialize(serialized: String): Any {
        val parcel = gson.fromJson(serialized, ParcelWithType::class.java)
        return when (parcel.t) {
            TYPENAME_INT -> parcel.v.toInt()
            TYPENAME_FLOAT -> parcel.v.toFloat()
            TYPENAME_DOUBLE -> parcel.v.toDouble()
            TYPENAME_LONG -> parcel.v.toLong()
            TYPENAME_STRING -> parcel.v
            TYPENAME_BIGDECIMAL -> BigDecimal(parcel.v)
            TYPENAME_TIMEPOINT -> TimePoint(parcel.v)
            TYPENAME_TIMESPAN -> TimeSpan(parcel.v)
            TYPENAME_INT_ARRAY -> if (parcel.v.isNullOrEmpty()) {
                intArrayOf()
            } else {
                parcel.v.split(",").map { it.toInt() }.toIntArray()
            }
            TYPENAME_LONG_ARRAY -> if (parcel.v.isNullOrEmpty()) {
                longArrayOf()
            } else {
                parcel.v.split(",").map { it.toLong() }.toLongArray()
            }
            TYPENAME_LATITUDE_LONGITUDE -> deserializeLatLng(parcel.v)
            TYPENAME_ROUTE -> Route(parcel.v)
            TYPENAME_URI -> Uri.parse(parcel.v)
            else -> return parcel.v
        }
    }

}