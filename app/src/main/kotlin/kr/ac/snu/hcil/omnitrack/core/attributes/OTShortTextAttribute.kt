package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 8. 1..
 */
class OTShortTextAttribute(objectId: String?, dbId: Long?, columnName: String, isRequired: Boolean, settingData: String?, connectionData: String?) : OTAttribute<CharSequence>(objectId, dbId, columnName, isRequired, Companion.TYPE_SHORT_TEXT, settingData, connectionData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
    override val typeNameResourceId: Int = R.string.type_shorttext_name
    override val typeSmallIconResourceId: Int = R.drawable.icon_small_shorttext

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, false)

    override fun createProperties() {
    }

    override val propertyKeys: IntArray = intArrayOf()

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (CharSequence) -> Unit): Boolean {
        resultHandler("")
        return true
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }
}