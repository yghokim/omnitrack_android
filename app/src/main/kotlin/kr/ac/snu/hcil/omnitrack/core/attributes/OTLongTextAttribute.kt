package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.LinedTextView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 7. 24..
 */
class OTLongTextAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?) : OTAttribute<CharSequence>(objectId, dbId, columnName, Companion.TYPE_LONG_TEXT, settingData, connectionData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_LONG_TEXT
    }

    override val propertyKeys: Array<Int> = Array<Int>(0) { index -> 0 }

    override val typeNameResourceId: Int = R.string.type_longtext_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_longtext


    override fun createProperties() {
    }

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (CharSequence) -> Unit): Boolean {
        resultHandler("")
        return true
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override fun getViewForItemList(context: Context, recycledView: View?): View {

        val target = if (recycledView is LinedTextView) {
            recycledView
        } else LinedTextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.longTextForItemListTextAppearance)

        target.setLineSpacing(context.resources.getDimension(R.dimen.item_list_element_LongText_LineSpacingExtra), 1.2f)

        target.base.drawOuterLines = false

        target.setBackgroundResource(R.drawable.longtext_item_list_background)
        //target.lineColor = context.resources.getColor(R.color.)

        return target
    }

    override fun getViewForItemListContainerType(): Int = VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
}