package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLongTextAttributeHelper : OTAttributeHelper() {

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

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_LONG_TEXT
}