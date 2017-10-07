package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTChoiceAttributeHelper : OTAttributeHelper() {
    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false) //TODO conditions

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.string.type_choice_name

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int = R.drawable.icon_small_multiple_choice //TODO conditions

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY
}