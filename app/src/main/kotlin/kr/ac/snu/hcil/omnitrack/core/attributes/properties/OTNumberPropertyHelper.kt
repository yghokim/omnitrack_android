package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.NumberPropertyView
import java.math.BigDecimal

/**
 * Created by younghokim on 2018. 1. 22..
 */
class OTNumberPropertyHelper : OTPropertyHelper<BigDecimal>() {

    override fun getSerializedValue(value: BigDecimal): String {
        return value.toPlainString()
    }

    override fun parseValue(serialized: String): BigDecimal {
        return BigDecimal(serialized)
    }

    override fun makeView(context: Context): APropertyView<BigDecimal> {
        return NumberPropertyView(context, null)
    }
}