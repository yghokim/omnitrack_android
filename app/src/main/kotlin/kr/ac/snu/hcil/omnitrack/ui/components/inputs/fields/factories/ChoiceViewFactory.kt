package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTChoiceFieldHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTChoiceEntryListPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.WordListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.ChoiceInputView
import java.util.*

class ChoiceViewFactory(helper: OTChoiceFieldHelper) : OTFieldViewFactory<OTChoiceFieldHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_CHOICE

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is ChoiceInputView) {
            inputView.entries = helper.getChoiceEntries(field)?.toArray() ?: emptyArray()
            inputView.multiSelectionMode = helper.getIsMultiSelectionAllowed(field) == true
            inputView.allowAppendingFromView = helper.getIsAppendingFromViewAllowed(field)
        }
    }

    override fun getInputView(context: Context, previewMode: Boolean, field: OTFieldDAO, recycledView: AFieldInputView<out Any>?): AFieldInputView<out Any> {
        val inputView = super.getInputView(context, previewMode, field, recycledView)
        if (inputView is ChoiceInputView) {
            if (inputView.entries.isEmpty()) {
                inputView.entries = (helper.propertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList) as OTChoiceEntryListPropertyHelper).previewChoiceEntries
            }
        }

        return inputView
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {
        val target: WordListView = recycledView as? WordListView ?: WordListView(context)

        target.useColors = true

        return target
    }

    override fun applyValueToViewForItemList(context: Context, field: OTFieldDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is WordListView) {
                view.colorIndexList.clear()

                if (value is IntArray && value.size > 0) {
                    val entries = helper.getChoiceEntries(field)
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
            } else super.applyValueToViewForItemList(context, field, value, view)
        }
    }
}