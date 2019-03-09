package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.android.common.isNumericPrimitive
import kr.ac.snu.hcil.android.common.toBigDecimal
import kr.ac.snu.hcil.omnitrack.core.datatypes.*
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-27.
 */

fun LatLng.serialize(): String {
    return "$latitude,$longitude"
}

fun deserializeLatLng(serialized: String): LatLng {
    val s = serialized.split(",")
    return LatLng(s[0].toDouble(), s[1].toDouble())
}

object TypeStringSerializationHelper {

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
    const val TYPENAME_FRACTION = "Fr"
    const val TYPENAME_SERVERFILE = "SF"


    val classNameDictionary: Map<String, String> = mapOf(
            Int::class.java.name to TYPENAME_INT,
            "java.lang.Integer" to TYPENAME_INT,
            Long::class.java.name to TYPENAME_LONG,
            "java.lang.Long" to TYPENAME_LONG,
            "java.lang.Number" to TYPENAME_FLOAT,
            "java.lang.Double" to TYPENAME_DOUBLE,
            BigDecimal::class.java.name to TYPENAME_BIGDECIMAL,
            TimePoint::class.java.name to TYPENAME_TIMEPOINT,
            TimeSpan::class.java.name to TYPENAME_TIMESPAN,
            String::class.java.name to TYPENAME_STRING,
            IntArray::class.java.name to TYPENAME_INT_ARRAY,
            LongArray::class.java.name to TYPENAME_LONG_ARRAY,
            LatLng::class.java.name to TYPENAME_LATITUDE_LONGITUDE,
            Route::class.java.name to TYPENAME_ROUTE,
            OTServerFile::class.java.name to TYPENAME_SERVERFILE,
            Fraction::class.java.name to TYPENAME_FRACTION
    )

    fun serialize(typeName: String, value: Any): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(typeName.length)
        stringBuilder.append(typeName)

        val stringValue = when (typeName) {
            TYPENAME_BIGDECIMAL -> {
                if (value is BigDecimal) {
                    value.toPlainString()
                } else {
                    if (isNumericPrimitive(value)) {
                        toBigDecimal(value).toPlainString()
                    } else throw IllegalArgumentException("input value $value is not convertible to BigDecimal.")
                }
            }
            TYPENAME_TIMEPOINT -> {
                (value as? TimePoint)?.getSerializedString() ?: if (value is Long) {
                    TimePoint(value, TimeZone.getDefault().id).getSerializedString()
                } else throw TypeCastException("this value is not a Long or Timepoint.")
            }
            TYPENAME_TIMESPAN -> (value as TimeSpan).getSerializedString()
            TYPENAME_INT_ARRAY -> (value as IntArray).joinToString(",")
            TYPENAME_LONG_ARRAY -> (value as IntArray).joinToString(",")
            TYPENAME_LATITUDE_LONGITUDE -> (value as LatLng).serialize()
            TYPENAME_ROUTE -> (value as Route).getSerializedString()
            TYPENAME_FRACTION -> {
                val f = value as Fraction
                "${f.upper}/${f.under}"
            }
            TYPENAME_SERVERFILE -> {
                (value as OTServerFile).getSerializedString()
            }
            else -> value.toString()
        }

        stringBuilder.append(stringValue)

        return stringBuilder.toString()
    }

    fun serialize(value: Any): String {
        if (classNameDictionary.containsKey(value.javaClass.name)) {
            return serialize(classNameDictionary[value.javaClass.name]!!, value)
        } else throw Exception("Serialization of this type [${value.javaClass.name}] is not implemented, value: $value")
    }

    fun deserialize(serialized: String): Any {
        val typeLength = Character.getNumericValue(serialized[0])
        val typeName = serialized.subSequence(1, typeLength + 1)
        val value = serialized.subSequence(1 + typeLength, serialized.length).toString()
        return when (typeName) {
            TYPENAME_INT -> value.toInt()
            TYPENAME_FLOAT -> value.toFloat()
            TYPENAME_DOUBLE -> value.toDouble()
            TYPENAME_LONG -> value.toLong()
            TYPENAME_STRING -> value
            TYPENAME_BIGDECIMAL -> BigDecimal(value)
            TYPENAME_TIMEPOINT -> TimePoint(value)
            TYPENAME_TIMESPAN -> TimeSpan(value)
            TYPENAME_INT_ARRAY -> if (value.isEmpty()) {
                intArrayOf()
            } else {
                value.split(",").map(String::toInt).toIntArray()
            }
            TYPENAME_LONG_ARRAY -> if (value.isEmpty()) {
                longArrayOf()
            } else {
                value.split(",").map(String::toLong).toLongArray()
            }
            TYPENAME_LATITUDE_LONGITUDE -> deserializeLatLng(value)
            TYPENAME_ROUTE -> Route(value)
            TYPENAME_SERVERFILE -> OTServerFile.typeAdapter.fromJson(value)
            TYPENAME_FRACTION -> {
                val split = value.split("/")
                Fraction(split[0].toShort(), split[1].toShort())
            }
            else -> return value
        }
    }

    fun isNumeric(typeCode: String): Boolean {
        return typeCode == TYPENAME_LONG ||
                typeCode == TYPENAME_BIGDECIMAL ||
                typeCode == TYPENAME_DOUBLE ||
                typeCode == TYPENAME_FLOAT ||
                typeCode == TYPENAME_INT
    }

}