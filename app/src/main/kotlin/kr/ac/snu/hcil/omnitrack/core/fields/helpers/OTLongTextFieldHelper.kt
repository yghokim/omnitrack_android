package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLongTextFieldHelper(context: Context) : ATextTypeFieldHelper(context) {

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics {
        return NumericCharacteristics(false, false)
    }

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_longtext_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return R.drawable.icon_small_longtext
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
}