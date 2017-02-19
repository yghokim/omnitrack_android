package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTNumberStyleProperty
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.toBigDecimal
import rx.Observable
import java.math.BigDecimal

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTNumberAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?) : OTAttribute<BigDecimal>(objectId, localKey, parentTracker, columnName, isRequired, OTAttribute.TYPE_NUMBER, settingData, connectionData) {

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_NUMBER
    }

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, true)

    override val typeNameResourceId: Int = R.string.type_number_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_number

    override val propertyKeys: Array<String> = arrayOf(NUMBERSTYLE)

    companion object {
        const val NUMBERSTYLE = "style"
    }

    override fun createProperties() {
        assignProperty(OTNumberStyleProperty(NUMBERSTYLE))
    }

    override fun compareValues(a: Any, b: Any): Int {
        try {
            val bdA = toBigDecimal(a)
            val bdB = toBigDecimal(b)
            return bdA.compareTo(bdB)
        } catch(e: Exception) {
            e.printStackTrace()
            return super.compareValues(a, b)
        }
    }
/*
    var numDigitsUnderDecimalPoint: Int
        get() = getPropertyValue<Int>(DECIMAL_POINTS)
        set(value) = setPropertyValue(DECIMAL_POINTS, value)

    var unit: String
        get() = getPropertyValue<String>(UNIT)
        set(value) = setPropertyValue(UNIT, value)

*/

    var numberStyle: NumberStyle
        get() = getPropertyValue<NumberStyle>(NUMBERSTYLE)
        set(value) = setPropertyValue(NUMBERSTYLE, value)

    override fun formatAttributeValue(value: Any): CharSequence {
        return numberStyle.formatNumber(value)
    }

    override fun getAutoCompleteValue(): Observable<BigDecimal> {
        return Observable.just(BigDecimal(0))
    }


    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = numberStyle
        }
    }

}