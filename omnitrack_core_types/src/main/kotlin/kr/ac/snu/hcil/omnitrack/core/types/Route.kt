package kr.ac.snu.hcil.omnitrack.core.types

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.android.common.serialization.IStringSerializable
import java.util.*

/**
 * Created by younghokim on 16. 8. 3..
 */
class Route() : ArrayList<LatLng>(), IStringSerializable {

    companion object {
        const val SEPARATOR = '|'
    }

    constructor(serialized: String) : this() {
        fromSerializedString(serialized)
    }

    override fun fromSerializedString(serialized: String): Boolean {
        clear()
        val coordStrings = serialized.split(SEPARATOR)
        for (coordString in coordStrings) {
            add(deserializeLatLng(coordString))
        }

        return true
    }

    override fun getSerializedString(): String {

        val stringBuilder = StringBuilder()

        for (coord in this) {
            stringBuilder.append(SEPARATOR)
            stringBuilder.append(coord.serialize())
        }

        return stringBuilder.toString()
    }
}

fun LatLng.getAddress(context: Context): Address? {

    val geocoder = Geocoder(context)
    try {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return addresses?.firstOrNull()
    } catch (e: Exception) {
        e.printStackTrace()

        return null
    }
}

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