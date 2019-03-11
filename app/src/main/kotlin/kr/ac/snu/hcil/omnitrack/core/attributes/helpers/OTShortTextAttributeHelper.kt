package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTShortTextAttributeHelper(context: Context) : ATextTypeAttributeHelper(context) {

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics {
        return NumericCharacteristics(false, false)
    }

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_shorttext_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_shorttext
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
}