package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

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

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }
}