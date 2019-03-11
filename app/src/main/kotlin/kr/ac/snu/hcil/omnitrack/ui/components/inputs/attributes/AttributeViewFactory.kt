package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.view.View
import android.widget.TextView
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.PropertyViewFactory
import java.util.*

abstract class AttributeViewFactory<HelperType : OTAttributeHelper>(protected val helper: HelperType) {
    abstract fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int

    open fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {}
    //reuse recycled view if possible.

    open fun getInputView(context: Context, previewMode: Boolean, attribute: OTAttributeDAO, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == getInputViewType(previewMode, attribute))) {
                    recycledView
                } else {
                    AAttributeInputView.makeInstance(getInputViewType(previewMode, attribute), context)
                }

        refreshInputViewUI(view, attribute)
        view.previewMode = previewMode
        return view
    }

    //Item list view==========================================================================================================================


    open fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE
    }

    open fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {

        val target: TextView = recycledView as? TextView ?: TextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.viewForItemListTextAppearance)

        target.background = null

        return target
    }

    open fun applyValueToViewForItemList(context: Context, attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer<Boolean> {
            if (view is TextView) {
                if (value != null) {
                    view.text = TextHelper.stringWithFallback(attribute.getHelper(context).formatAttributeValue(attribute, value), "-")
                } else {
                    view.text = view.context.getString(R.string.msg_empty_value)
                }
                Single.just(true)
            } else Single.just(false)
        }
    }


    open fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val view = PropertyViewFactory.makeView(helper.getPropertyHelper<Any>(propertyKey), context)
        view.title = helper.getPropertyTitle(propertyKey)
        val initialValue = helper.getPropertyInitialValue(propertyKey)
        if (initialValue != null)
            view.value = initialValue
        return view
    }

    open fun makePropertyViews(context: Context): Collection<Pair<String, View>> {
        val result = ArrayList<Pair<String, View>>()
        for (key in helper.propertyKeys) {
            result.add(Pair(key, makePropertyView(key, context)))
        }
        return result
    }
}