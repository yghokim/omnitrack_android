package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import android.view.View
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTChoiceEntryListPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.WordListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import java.util.*

class ChoiceViewFactory(helper: OTChoiceAttributeHelper) : AttributeViewFactory<OTChoiceAttributeHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_CHOICE

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ChoiceInputView) {
            inputView.entries = helper.getChoiceEntries(attribute)?.toArray() ?: emptyArray()
            inputView.multiSelectionMode = helper.getIsMultiSelectionAllowed(attribute) == true
            inputView.allowAppendingFromView = helper.getIsAppendingFromViewAllowed(attribute)
        }
    }

    override fun getInputView(context: Context, previewMode: Boolean, attribute: OTAttributeDAO, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val inputView = super.getInputView(context, previewMode, attribute, recycledView)
        if (inputView is ChoiceInputView) {
            if (inputView.entries.isEmpty()) {
                inputView.entries = (helper.propertyManager.getHelper(OTPropertyManager.EPropertyType.ChoiceEntryList) as OTChoiceEntryListPropertyHelper).previewChoiceEntries
            }
        }

        return inputView
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        val target: WordListView = recycledView as? WordListView ?: WordListView(context)

        target.useColors = true

        return target
    }

    override fun applyValueToViewForItemList(context: Context, attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is WordListView) {
                view.colorIndexList.clear()

                if (value is IntArray && value.size > 0) {
                    val entries = helper.getChoiceEntries(attribute)
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
            } else super.applyValueToViewForItemList(context, attribute, value, view)
        }
    }
}