package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView

/**
 * Created by younghokim on 16. 8. 12..
 */
class OTBooleanPropertyHelper : OTPropertyHelper<Boolean>() {
    override fun getSerializedValue(value: Boolean): String {
        return value.toString()
    }

    override fun parseValue(serialized: String): Boolean {
        return serialized.toBoolean()
    }

    override fun onBuildView(context: Context): APropertyView<Boolean> {
        return BooleanPropertyView(context, null)
    }

}