package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTNumberAttributeHelper : OTAttributeHelper() {

    companion object {
        const val NUMBERSTYLE = "style"
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_number_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_number
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override val propertyKeys: Array<String>
        get() = arrayOf(NUMBERSTYLE)

    override fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T? {
        return when (propertyKey) {
            NUMBERSTYLE -> {
                OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.NumberStyle).parseValue(serializedValue)
            }
            else -> throw IllegalArgumentException("Unsupported property key.")
        } as T
    }

    private fun getNumberStyle(attribute: OTAttributeDAO): NumberStyle? {
        return getDeserializedPropertyValue(NUMBERSTYLE, attribute)
    }


    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_NUMBER

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = getNumberStyle(attribute) ?: NumberStyle()
        }
    }
}