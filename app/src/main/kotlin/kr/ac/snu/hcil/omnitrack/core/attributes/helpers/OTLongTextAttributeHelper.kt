package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.text.LinedTextView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLongTextAttributeHelper(context: Context) : ATextTypeAttributeHelper(context) {

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics {
        return NumericCharacteristics(false, false)
    }

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_longtext_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_longtext
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_LONG_TEXT

    //item list=====================================================

    override fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        val target = recycledView as? LinedTextView ?: LinedTextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.longTextForItemListTextAppearance)

        target.setLineSpacing(context.resources.getDimension(R.dimen.item_list_element_longText_lineSpacingExtra), 1.2f)

        target.base.drawOuterLines = false

        target.setBackgroundResource(R.drawable.longtext_item_list_background)
        //target.lineColor = context.resources.getColor(R.color.)

        return target
    }
}