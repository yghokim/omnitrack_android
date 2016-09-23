package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.RatingOptionsPropertyView
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class OTRatingOptionsProperty(key: Int) : OTProperty<RatingOptions>(RatingOptions(), key, null) {

    override fun parseValue(serialized: String): RatingOptions {
        return Gson().fromJson(serialized, RatingOptions::class.java)
    }

    override fun onBuildView(context: Context): APropertyView<RatingOptions> {
        return RatingOptionsPropertyView(context, null)
    }

    override fun getSerializedValue(): String {
        return Gson().toJson(value)
    }
}