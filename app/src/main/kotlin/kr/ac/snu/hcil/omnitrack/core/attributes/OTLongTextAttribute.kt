package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 7. 24..
 */
class OTLongTextAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?) : OTAttribute<CharSequence>(objectId, dbId, columnName, Companion.TYPE_LONG_TEXT, settingData, connectionData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_LONG_TEXT
    }

    override val propertyKeys: Array<Int> = Array<Int>(0) { index -> 0 }

    override val typeNameResourceId: Int = R.string.type_longtext_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_longtext


    override fun createProperties() {
    }

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (CharSequence) -> Unit) {
        resultHandler("")
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {

    }
}