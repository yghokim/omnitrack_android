package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTBooleanProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListProperty
import kr.ac.snu.hcil.omnitrack.ui.components.WordListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by younghokim on 16. 8. 12..
 */
class OTChoiceAttribute(objectId: String?, dbId: Long?, columnName: String, propertyData: String?, connectionData: String?) : OTAttribute<IntArray>(objectId, dbId, columnName, TYPE_CHOICE, propertyData, connectionData) {

    companion object {
        const val PROPERTY_MULTISELECTION = 0
        const val PROPERTY_ENTRIES = 1

        val PREVIEW_ENTRIES: Array<String>

        init {
            PREVIEW_ENTRIES = OmniTrackApplication.app.resources.getStringArray(R.array.choice_preview_entries)
        }
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
        if (value is IntArray && value.size > 0) {
            val builder = StringBuilder()
            for (e in value.withIndex()) {
                if (e.value < entries.size) {
                    builder.append(entries[e.value])
                    if (e.index < value.size - 1) {
                        builder.append(", ")
                    }
                }
            }

            return builder.toString()
        } else return "No selection"
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
            inputView.entries = entries
            inputView.multiSelectionMode = allowedMultiselection
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
            if (value is IntArray && value.size > 0) {
                val list = ArrayList <String>()
                for (e in value.withIndex()) {
                    if (e.value < entries.size) {
                        list.add(entries[e.value])
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

        return target
    }


}