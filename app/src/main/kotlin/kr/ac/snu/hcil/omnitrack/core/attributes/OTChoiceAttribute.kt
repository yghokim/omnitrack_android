package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTBooleanProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListProperty
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
 * Created by younghokim on 16. 8. 12..
 */
class OTChoiceAttribute(objectId: String?, dbId: Long?, columnName: String, propertyData: String?, connectionData: String?) : OTAttribute<IntArray>(objectId, dbId, columnName, TYPE_CHOICE, propertyData, connectionData) {

    companion object {
        const val PROPERTY_MULTISELECTION = 0
        const val PROPERTY_ENTRIES = 1

        val PREVIEW_ENTRIES: Array<UniqueStringEntryList.Entry>

        init {
            PREVIEW_ENTRIES = OTApplication.app.resources.getStringArray(R.array.choice_preview_entries).mapIndexed {
                i, s ->
                UniqueStringEntryList.Entry(i, s)
            }.toTypedArray()
        }
    }

    override val propertyKeys: IntArray = intArrayOf(PROPERTY_MULTISELECTION, PROPERTY_ENTRIES)

    override val typeNameResourceId: Int = R.string.type_choice_name

    override val typeSmallIconResourceId: Int
        get() {
            if (allowedMultiSelection) {
                return R.drawable.icon_small_multiple_choice
            } else {
                return R.drawable.icon_small_single_choice
            }
        }

    override val valueNumericCharacteristics: NumericCharacteristics
        get() {
            return if (allowedMultiSelection)
                NumericCharacteristics(false, false)
            else NumericCharacteristics(true, false)
        }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_INT_ARRAY


    override fun createProperties() {
        assignProperty(OTBooleanProperty(false, PROPERTY_MULTISELECTION, "Allow Multiple Selections"))
        assignProperty(OTChoiceEntryListProperty(PROPERTY_ENTRIES, "Entries"))
    }

    var allowedMultiSelection: Boolean
        get() = getPropertyValue(PROPERTY_MULTISELECTION)
        set(value) = setPropertyValue(PROPERTY_MULTISELECTION, value)

    var entries: UniqueStringEntryList
        get() = getPropertyValue(PROPERTY_ENTRIES)
        set(value) = setPropertyValue(PROPERTY_ENTRIES, value)

    override fun formatAttributeValue(value: Any): String {
        val entries = this.entries
        if (value is IntArray && value.size > 0) {
            val builder = StringBuilder()
            for (e in value.withIndex()) {
                /*
                if (e.value < entries.size) {
                    builder.append(entries[e.value])
                    if (e.index < value.size - 1) {
                        builder.append(", ")
                    }
                }*/
                val entry = entries.findWithId(e.value)
                if (entry != null) {
                    builder.append(entry.text)
                    builder.append(", ")
                }
            }

            return builder.substring(0, -2)
        } else return "No selection"
    }

    override fun compareValues(a: Any, b: Any): Int {
        if (a is IntArray && b is IntArray) {
            if (a.size > 0 && b.size > 0) {
                val idA = a.first()
                val idB = b.first()

                val valueA = entries.findWithId(idA)
                val valueB = entries.findWithId(idB)
                if (valueA != null && valueB != null) {
                    println("${valueA} compare ${valueB} :  ${valueA.text.compareTo(valueB.text)}")
                    return valueA.text.compareTo(valueB.text)
                } else if (valueA == null) {
                    return -1
                } else if (valueB == null) {
                    return 1
                } else return 0

            } else if (a.size == 0) {
                return -1
            } else if (b.size == 0) {
                return 1
            } else return 0
        }
        return super.compareValues(a, b)
    }

    override fun getAutoCompleteValueAsync(resultHandler: (IntArray) -> Unit): Boolean {
        resultHandler.invoke(IntArray(0))
        return true
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_CHOICE
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if (inputView is ChoiceInputView) {
            inputView.entries = entries.toArray()
            inputView.multiSelectionMode = allowedMultiSelection
        }
    }

    override fun getInputView(context: Context, previewMode: Boolean, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val inputView = super.getInputView(context, previewMode, recycledView)
        if (inputView is ChoiceInputView) {
            if (inputView.entries.isEmpty()) {
                inputView.entries = PREVIEW_ENTRIES
            }
        }

        return inputView
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is WordListView) {
            view.colorIndexList.clear()

            if (value is IntArray && value.size > 0) {
                val list = ArrayList <String>()
                for (idEntry in value.withIndex()) {

                    val indexInEntries = entries.indexOf(idEntry.value)
                    if (indexInEntries >= 0) {
                        list.add(entries[indexInEntries].text)
                        view.colorIndexList.add(indexInEntries)
                    }
                }

                view.words = list.toTypedArray()

                return true
            } else {
                view.words = arrayOf()
                return true
            }
        } else return super.applyValueToViewForItemList(value, view)
    }

    override fun getViewForItemListContainerType(): Int = OTAttribute.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE

    override fun getViewForItemList(context: Context, recycledView: View?): View {

        val target: WordListView = if (recycledView is WordListView) {
            recycledView
        } else WordListView(context)


        target.useColors = true

        return target
    }

    override fun getRecommendedChartModels(): Array<ChartModel<*>> {
        return arrayOf(ChoiceCategoricalBarChartModel(this))
    }


}