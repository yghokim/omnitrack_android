package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.text.LinedTextView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable

/**
 * Created by younghokim on 16. 7. 24..
 */
class OTLongTextAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: String?, connectionData: String?) : OTAttribute<CharSequence>(objectId, localKey, parentTracker, columnName, isRequired, Companion.TYPE_LONG_TEXT, settingData, connectionData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_LONG_TEXT
    }

    override val propertyKeys: IntArray = intArrayOf()

    override val typeNameResourceId: Int = R.string.type_longtext_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_longtext

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, false)

    override fun createProperties() {
    }

    override fun formatAttributeValue(value: Any): CharSequence {
        return value.toString()
    }

    override fun getAutoCompleteValue(): Observable<CharSequence> {
        return Observable.just("")
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override fun getViewForItemList(context: Context, recycledView: View?): View {

        val target = recycledView as? LinedTextView ?: LinedTextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.longTextForItemListTextAppearance)

        target.setLineSpacing(context.resources.getDimension(R.dimen.item_list_element_LongText_LineSpacingExtra), 1.2f)

        target.base.drawOuterLines = false

        target.setBackgroundResource(R.drawable.longtext_item_list_background)
        //target.lineColor = context.resources.getColor(R.color.)

        return target
    }

    override fun getViewForItemListContainerType(): Int = VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
}