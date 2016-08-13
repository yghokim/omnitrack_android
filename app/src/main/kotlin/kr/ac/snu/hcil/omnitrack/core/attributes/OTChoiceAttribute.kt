package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTBooleanProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListProperty
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 8. 12..
 */
class OTChoiceAttribute(objectId: String?, dbId: Long?, columnName: String, propertyData: String?, connectionData: String?) : OTAttribute<IntArray>(objectId, dbId, columnName, TYPE_CHOICE, propertyData, connectionData) {

    companion object {
        const val PROPERTY_MULTISELECTION = 0
        const val PROPERTY_ENTRIES = 1
    }

    override val propertyKeys: Array<Int> = arrayOf(PROPERTY_MULTISELECTION, PROPERTY_ENTRIES)

    override val typeNameResourceId: Int = R.string.type_choice_name

    override val typeSmallIconResourceId: Int
        get() {
            if (allowedMultiselection) {
                return R.drawable.icon_small_multiple_choice
            } else {
                return R.drawable.icon_small_single_choice
            }
        }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY


    override fun createProperties() {
        assignProperty(OTBooleanProperty(false, PROPERTY_MULTISELECTION, "Allow Multiple Selections"))
        assignProperty(OTChoiceEntryListProperty(PROPERTY_ENTRIES, "Entries"))
    }

    var allowedMultiselection: Boolean
        get() = getPropertyValue(PROPERTY_MULTISELECTION)
        set(value) = setPropertyValue(PROPERTY_MULTISELECTION, value)

    var entries: Array<String>
        get() = getPropertyValue(PROPERTY_ENTRIES)
        set(value) = setPropertyValue(PROPERTY_ENTRIES, value)

    override fun formatAttributeValue(value: Any): String {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAutoCompleteValueAsync(resultHandler: (IntArray) -> Unit) {
        resultHandler.invoke(IntArray(0))
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_NUMBER
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {

    }
}