package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 8. 1..
 */
class OTShortTextAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<CharSequence>(objectId, dbId, columnName, Companion.TYPE_SHORT_TEXT, settingData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
    override val typeNameResourceId: Int = R.string.type_shorttext_name

    override fun createProperties() {
    }

    override val keys: Array<Int> = Array<Int>(0) { index -> 0 }

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (CharSequence) -> Unit) {
        resultHandler("")
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {

    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }
}