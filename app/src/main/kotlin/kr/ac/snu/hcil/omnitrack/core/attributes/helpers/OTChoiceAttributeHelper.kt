package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.view.View
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ChoiceSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.ChoiceCategoricalBarChartModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.WordListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTChoiceAttributeHelper(configuredContext: ConfiguredContext) : OTAttributeHelper(configuredContext) {

    companion object {
        const val PROPERTY_MULTISELECTION = "multiSelection"
        const val PROPERTY_ENTRIES = "entries"
        const val PROPERTY_ALLOW_APPENDING_FROM_VIEW = "allowAppendingFromView"
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics {
        return if (getIsMultiSelectionAllowed(attribute) == true)
            NumericCharacteristics(false, false)
        else NumericCharacteristics(true, false)
    }

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.string.type_choice_name

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return if (getIsMultiSelectionAllowed(attribute) == true) {
            R.drawable.icon_small_multiple_choice
        } else R.drawable.icon_small_single_choice
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(
                ChoiceSorter(attribute.name, getChoiceEntries(attribute)
                        ?: UniqueStringEntryList(), attribute.localId)
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
            PROPERTY_MULTISELECTION -> configuredContext.applicationContext.getString(R.string.property_choice_allow_multiple_selections)
            PROPERTY_ALLOW_APPENDING_FROM_VIEW -> configuredContext.applicationContext.getString(R.string.msg_allow_appending_from_view)
            PROPERTY_ENTRIES -> configuredContext.applicationContext.getString(R.string.property_choice_entries)
            else -> ""
        }
    }

    private fun getIsMultiSelectionAllowed(attribute: OTAttributeDAO): Boolean? {
        return getDeserializedPropertyValue<Boolean>(PROPERTY_MULTISELECTION, attribute)
    }

    private fun getIsAppendingFromViewAllowed(attribute: OTAttributeDAO): Boolean {
        return getDeserializedPropertyValue<Boolean>(PROPERTY_ALLOW_APPENDING_FROM_VIEW, attribute)
                ?: false
    }

    fun getChoiceEntries(attribute: OTAttributeDAO): UniqueStringEntryList? {
        return getDeserializedPropertyValue(PROPERTY_ENTRIES, attribute)
    }

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_CHOICE

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ChoiceInputView) {
            inputView.entries = getChoiceEntries(attribute)?.toArray() ?: emptyArray()
            inputView.multiSelectionMode = getIsMultiSelectionAllowed(attribute) == true
            inputView.allowAppendingFromView = getIsAppendingFromViewAllowed(attribute)
        }
    }

    override fun getInputView(context: Context, previewMode: Boolean, attribute: OTAttributeDAO, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val inputView = super.getInputView(context, previewMode, attribute, recycledView)
        if (inputView is ChoiceInputView) {
            if (inputView.entries.isEmpty()) {
                inputView.entries = (propertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList) as OTChoiceEntryListPropertyHelper).previewChoiceEntries
            }
        }

        return inputView
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        val target: WordListView = recycledView as? WordListView ?: WordListView(context)

        target.useColors = true

        return target
    }

    override fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is WordListView) {
                view.colorIndexList.clear()

                if (value is IntArray && value.size > 0) {
                    val entries = getChoiceEntries(attribute)
                    if (entries != null) {
                        val list = ArrayList<String>()
                        for (idEntry in value.withIndex()) {
                            val indexInEntries = entries.indexOf(idEntry.value)
                            if (indexInEntries >= 0) {
                                list.add(entries[indexInEntries].text)
                                view.colorIndexList.add(indexInEntries)
                            }
                        }

                        view.words = list.toTypedArray()
                    }
                } else {
                    view.words = arrayOf()
                }
                Single.just(true)
            } else super.applyValueToViewForItemList(attribute, value, view)
        }
    }

    override fun makeRecommendedChartModels(attribute: OTAttributeDAO, realm: Realm): Array<ChartModel<*>> {
        return arrayOf(ChoiceCategoricalBarChartModel(attribute, realm, configuredContext))
    }

    private fun getChoiceTexts(attribute: OTAttributeDAO, value: IntArray): List<String> {
        val entries = getChoiceEntries(attribute)
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

    override fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        if (value is IntArray) {
            return getChoiceTexts(attribute, value).joinToString(",")
        } else return super.formatAttributeValue(attribute, value)
    }

    override fun onAddValueToTable(attribute: OTAttributeDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (value is IntArray) {
            out.add(getChoiceTexts(attribute, value).joinToString(","))
        } else out.add(null)
    }
}