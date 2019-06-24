package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.text.LinedTextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTLongTextFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory

class LongTextViewFactory(helper: OTLongTextFieldHelper) : OTFieldViewFactory<OTLongTextFieldHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_LONG_TEXT

    //item list=====================================================

    override fun getViewForItemListContainerType(): Int {
        return OTFieldManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {
        val target = recycledView as? LinedTextView
                ?: LinedTextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.longTextForItemListTextAppearance)

        target.setLineSpacing(context.resources.getDimension(R.dimen.item_list_element_longText_lineSpacingExtra), 1.2f)

        target.base.drawOuterLines = false

        target.setBackgroundResource(R.drawable.longtext_item_list_background)
        //target.lineColor = context.resources.getColor(R.color.)

        return target
    }
}