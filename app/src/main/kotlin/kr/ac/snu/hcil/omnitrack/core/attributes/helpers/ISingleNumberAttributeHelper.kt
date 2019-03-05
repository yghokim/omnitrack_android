package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO

/**
 * Created by younghokim on 2017. 11. 25..
 */
interface ISingleNumberAttributeHelper {
    fun convertValueToSingleNumber(value: Any, attribute: OTAttributeDAO): Double
}