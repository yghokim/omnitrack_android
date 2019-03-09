package kr.ac.snu.hcil.omnitrack.core.datatypes

import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.android.common.serialization.IStringSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.deserializeLatLng
import kr.ac.snu.hcil.omnitrack.utils.serialization.serialize
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