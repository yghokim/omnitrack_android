package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.UniqueStringEntryList
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.ChoiceSorter
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTChoiceEntryListPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.ChoiceCategoricalBarChartModel
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTChoiceFieldHelper(context: Context) : OTFieldHelper(context) {

    companion object {
        const val PROPERTY_MULTISELECTION = "multiSelection"
        const val PROPERTY_ENTRIES = "entries"
        const val PROPERTY_ALLOW_APPENDING_FROM_VIEW = "allowAppendingFromView"
    }

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics {
        return if (getIsMultiSelectionAllowed(field) == true)
            NumericCharacteristics(false, false)
        else NumericCharacteristics(true, false)
    }

    override fun getTypeNameResourceId(field: OTFieldDAO): Int = R.string.type_choice_name

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return if (getIsMultiSelectionAllowed(field) == true) {
            R.drawable.icon_small_multiple_choice
        } else R.drawable.icon_small_single_choice
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY

    override fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return arrayOf(
                ChoiceSorter(field.name, getChoiceEntries(field)
                        ?: UniqueStringEntryList(), field.localId)
        )
    }

    override val propertyKeys: Array<String> = arrayOf(
            PROPERTY_MULTISELECTION, PROPERTY_ALLOW_APPENDING_FROM_VIEW, PROPERTY_ENTRIES)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> propertyManager.getHelper(OTPropertyManager.EPropertyType.Boolean)
            PROPERTY_ALLOW_APPENDING_FROM_VIEW -> propertyManager.getHelper(OTPropertyManager.EPropertyType.Boolean)
            PROPERTY_ENTRIES -> propertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList)
            else -> throw IllegalArgumentException("Unsupported property key $propertyKey")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> false
            PROPERTY_ALLOW_APPENDING_FROM_VIEW -> false
            PROPERTY_ENTRIES -> UniqueStringEntryList((propertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList) as OTChoiceEntryListPropertyHelper).previewChoiceEntries)
            else -> null
        }
    }


    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            PROPERTY_MULTISELECTION -> context.applicationContext.getString(R.string.property_choice_allow_multiple_selections)
            PROPERTY_ALLOW_APPENDING_FROM_VIEW -> context.applicationContext.getString(R.string.msg_allow_appending_from_view)
            PROPERTY_ENTRIES -> context.applicationContext.getString(R.string.property_choice_entries)
            else -> ""
        }
    }

    fun getIsMultiSelectionAllowed(field: OTFieldDAO): Boolean? {
        return getDeserializedPropertyValue<Boolean>(PROPERTY_MULTISELECTION, field)
    }

    fun getIsAppendingFromViewAllowed(field: OTFieldDAO): Boolean {
        return getDeserializedPropertyValue<Boolean>(PROPERTY_ALLOW_APPENDING_FROM_VIEW, field)
                ?: false
    }

    fun getChoiceEntries(field: OTFieldDAO): UniqueStringEntryList? {
        return getDeserializedPropertyValue(PROPERTY_ENTRIES, field)
    }

    override fun makeRecommendedChartModels(field: OTFieldDAO, realm: Realm): Array<ChartModel<*>> {
        return arrayOf(ChoiceCategoricalBarChartModel(field, realm, context))
    }

    private fun getChoiceTexts(field: OTFieldDAO, value: IntArray): List<String> {
        val entries = getChoiceEntries(field)
        val list = ArrayList<String>()
        if (entries != null) {
            for (idEntry in value.withIndex()) {

                val indexInEntries = entries.indexOf(idEntry.value)
                if (indexInEntries >= 0) {
                    list.add(entries[indexInEntries].text)
                }
            }
        }
        return list
    }

    override fun formatAttributeValue(field: OTFieldDAO, value: Any): CharSequence {
        if (value is IntArray) {
            return getChoiceTexts(field, value).joinToString(",")
        } else return super.formatAttributeValue(field, value)
    }

    override fun onAddValueToTable(field: OTFieldDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (value is IntArray) {
            out.add(getChoiceTexts(field, value).joinToString(","))
        } else out.add(null)
    }
}