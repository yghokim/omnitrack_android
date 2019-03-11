package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLongTextAttributeHelper(context: Context) : ATextTypeAttributeHelper(context) {

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics {
        return NumericCharacteristics(false, false)
    }

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_longtext_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_longtext
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
}