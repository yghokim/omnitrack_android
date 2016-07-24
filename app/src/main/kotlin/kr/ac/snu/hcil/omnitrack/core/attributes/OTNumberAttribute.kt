package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.util.SparseArray
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTNumberAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<Float>(objectId, dbId, columnName, OTAttribute.TYPE_NUMBER, settingData) {

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_NUMBER
    }

    override val typeNameResourceId: Int = R.string.type_number_name

    override val keys: Array<Int>
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


    override fun parseAttributeValue(storedValue: String): Float {
        return storedValue.toFloat()
    }

    override fun formatAttributeValue(value: Any): String {
        if (value is Float) {
            val power = Math.pow(10.0, numDigitsUnderDecimalPoint.toDouble())
            val numberStr = (Math.floor(power * value) / power).toString()

            return if (unit.isNullOrBlank()) {
                numberStr
            } else {
                "${numberStr} ${unit}"
            }
        } else {
            return ""
        }
    }

    override fun makeDefaultValue(): Float {
        return 0.0f
    }

}