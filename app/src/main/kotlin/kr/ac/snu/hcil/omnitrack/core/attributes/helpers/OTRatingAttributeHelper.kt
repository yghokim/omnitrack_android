package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTRatingAttributeHelper : OTAttributeHelper() {

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_rating_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_star //TODO Options
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_FLOAT

}