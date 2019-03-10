package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.types.NumberStyle

/**
 * Created by Young-Ho Kim on 2016-09-05.
 */
class OTNumberStylePropertyHelper : OTPropertyHelper<NumberStyle>() {
    override fun getSerializedValue(value: NumberStyle): String {
        return NumberStyle.typeAdapter.toJson(value)
    }

    override fun parseValue(serialized: String): NumberStyle {
        return try {
            NumberStyle.typeAdapter.fromJson(serialized)
        } catch (ex: Exception) {
            println("NumberStyle parsing error - 1st")
            ex.printStackTrace()
            Gson().fromJson(serialized)
        } catch (ex: Exception) {
            println("NumberStyle naive parsing error - return new object")
            ex.printStackTrace()
            NumberStyle()
        }
    }

}