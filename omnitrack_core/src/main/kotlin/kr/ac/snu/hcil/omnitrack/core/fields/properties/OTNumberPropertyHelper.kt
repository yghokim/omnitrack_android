package kr.ac.snu.hcil.omnitrack.core.fields.properties

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

}