package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable

/**
 * Created by younghokim on 16. 8. 1..
 */
class OTShortTextAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any>?, connectionData: String?) : OTAttribute<CharSequence>(objectId, localKey, parentTracker, columnName, isRequired, Companion.TYPE_SHORT_TEXT, settingData, connectionData) {

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING
    override val typeNameResourceId: Int = R.string.type_shorttext_name
    override val typeSmallIconResourceId: Int = R.drawable.icon_small_shorttext

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, false)

    override fun createProperties() {
    }

    override val propertyKeys: IntArray = intArrayOf()

    override fun formatAttributeValue(value: Any): CharSequence {
        return value.toString()
    }

    override fun getAutoCompleteValue(): Observable<CharSequence> {
        return Observable.just("")
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }

    override fun compareValues(a: Any, b: Any): Int {
        return a.toString().trim().compareTo(b.toString().trim(), true)
    }
}