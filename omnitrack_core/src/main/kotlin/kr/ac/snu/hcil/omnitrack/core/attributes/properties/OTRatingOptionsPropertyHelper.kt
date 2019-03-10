package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class OTRatingOptionsPropertyHelper : OTPropertyHelper<RatingOptions>() {
    override fun getSerializedValue(value: RatingOptions): String {
        return RatingOptions.typeAdapter.toJson(value)
    }

    override fun parseValue(serialized: String): RatingOptions {
        return try {
            RatingOptions.typeAdapter.fromJson(serialized)
        } catch (ex: Exception) {
            println("RatingOptions parsing error - 1st")
            ex.printStackTrace()
            Gson().fromJson(serialized)
        } catch (ex: Exception) {
            println("RatingOptions naive parsing error - return new object")
            ex.printStackTrace()
            RatingOptions()
        }
    }

}