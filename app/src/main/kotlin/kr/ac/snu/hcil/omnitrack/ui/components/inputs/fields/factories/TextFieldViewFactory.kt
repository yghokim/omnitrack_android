package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.text.LinedTextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTTextFieldHelper
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.views.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.SelectionPropertyView

class TextFieldViewFactory(helper: OTTextFieldHelper) : OTFieldViewFactory<OTTextFieldHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int {
        return when (helper.getInputType(field)) {
            OTTextFieldHelper.INPUT_TYPE_SHORT -> AFieldInputView.VIEW_TYPE_SHORT_TEXT
            OTTextFieldHelper.INPUT_TYPE_LONG -> AFieldInputView.VIEW_TYPE_LONG_TEXT
            else -> AFieldInputView.VIEW_TYPE_SHORT_TEXT
        }
    }

    override fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val superView = super.makePropertyView(propertyKey, context)
        if (propertyKey == OTTextFieldHelper.PROPERTY_INPUT_TYPE && superView is SelectionPropertyView) {
            superView.setEntries(arrayOf(
                    context.getString(R.string.msg_text_field_input_type_short),
                    context.getString(R.string.msg_text_field_input_type_long)))
        }

        return superView
    }


    //item list=====================================================

    override fun getViewForItemListContainerType(): Int {
        return OTFieldManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {

        if (helper.getInputType(field) == OTTextFieldHelper.INPUT_TYPE_LONG) {
            val target = recycledView as? LinedTextView
                    ?: LinedTextView(context)

            InterfaceHelper.setTextAppearance(target, R.style.longTextForItemListTextAppearance)

            target.setLineSpacing(context.resources.getDimension(R.dimen.item_list_element_longText_lineSpacingExtra), 1.2f)

            target.base.drawOuterLines = false

            target.setBackgroundResource(R.drawable.longtext_item_list_background)
            //target.lineColor = context.resources.getColor(R.color.)

            return target
        } else return super.getViewForItemList(field, context, recycledView)
    }

}