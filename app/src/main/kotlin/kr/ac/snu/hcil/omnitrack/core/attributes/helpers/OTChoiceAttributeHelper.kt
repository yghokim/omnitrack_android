package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTChoiceAttributeHelper : OTAttributeHelper() {

    companion object {
        const val PROPERTY_MULTISELECTION = "multiSelection"
        const val PROPERTY_ENTRIES = "entries"
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics {
        return if (getAllowedMultiSelection(attribute) == true)
            NumericCharacteristics(false, false)
        else NumericCharacteristics(true, false)
    }

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.string.type_choice_name

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return if (getAllowedMultiSelection(attribute) == true) {
            R.drawable.icon_small_multiple_choice
        } else R.drawable.icon_small_single_choice
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_MULTISELECTION, PROPERTY_ENTRIES)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Boolean)
            PROPERTY_ENTRIES -> OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList)
            else -> throw IllegalArgumentException("Unsupported property key ${propertyKey}")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> false
            PROPERTY_ENTRIES -> UniqueStringEntryList()
            else -> null
        }
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> OTApplication.getString(R.string.property_choice_allow_multiple_selections)
            PROPERTY_ENTRIES -> OTApplication.getString(R.string.property_choice_entries)
            else -> ""
        }
    }

    private fun getAllowedMultiSelection(attribute: OTAttributeDAO): Boolean? {
        return getDeserializedPropertyValue<Boolean>(PROPERTY_MULTISELECTION, attribute)
    }

    private fun getEntries(attribute: OTAttributeDAO): UniqueStringEntryList? {
        return getDeserializedPropertyValue<UniqueStringEntryList>(PROPERTY_ENTRIES, attribute)
    }

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_CHOICE

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ChoiceInputView) {
            inputView.entries = getEntries(attribute)?.toArray() ?: emptyArray()
            inputView.multiSelectionMode = getAllowedMultiSelection(attribute) ?: false
        }
    }

    override fun getInputView(context: Context, previewMode: Boolean, attribute: OTAttributeDAO, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val inputView = super.getInputView(context, previewMode, attribute, recycledView)
        if (inputView is ChoiceInputView) {
            if (inputView.entries.isEmpty()) {
                inputView.entries = OTChoiceEntryListPropertyHelper.PREVIEW_ENTRIES
            }
        }

        return inputView
    }

}