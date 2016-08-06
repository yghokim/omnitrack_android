package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.math.BigDecimal

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTNumberAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<BigDecimal>(objectId, dbId, columnName, OTAttribute.TYPE_NUMBER, settingData) {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_NUMBER
    }

    override val typeNameResourceId: Int = R.string.type_number_name

    override val propertyKeys: Array<Int>
        get() = arrayOf(DECIMAL_POINTS)

    constructor(columnName: String) : this(null, null, columnName, null)

    companion object{
        const val UNIT = 0
        const val DECIMAL_POINTS = 1
    }

    override fun createProperties() {
        assignProperty(OTSelectionProperty(DECIMAL_POINTS, "Digits under Decimal Points", arrayOf("None", "1", "2", "3")))
    }

    var numDigitsUnderDecimalPoint: Int
        get() = getPropertyValue<Int>(DECIMAL_POINTS)
        set(value) = setPropertyValue(DECIMAL_POINTS, value)

    var unit: String
        get() = getPropertyValue<String>(UNIT)
        set(value) = setPropertyValue(UNIT, value)




    override fun formatAttributeValue(value: Any): String {
        if (value is BigDecimal) {
            return value.toPlainString()
        } else return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (BigDecimal) -> Unit) {
        resultHandler(BigDecimal(0))
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {
        if (inputView is NumberInputView) {
            this.getAutoCompleteValueAsync {
                result ->
                inputView.value = result
            }
        }
    }

}