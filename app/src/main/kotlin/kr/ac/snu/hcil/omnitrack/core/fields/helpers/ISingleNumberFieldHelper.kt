package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by younghokim on 2017. 11. 25..
 */
interface ISingleNumberFieldHelper {
    fun convertValueToSingleNumber(value: Any, field: OTFieldDAO): Double
}