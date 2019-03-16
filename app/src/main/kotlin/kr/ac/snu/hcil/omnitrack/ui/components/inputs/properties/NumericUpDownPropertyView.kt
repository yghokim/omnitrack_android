package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.views.time.INumericUpDown
import kr.ac.snu.hcil.omnitrack.views.time.NumericUpDown

/**
 * Created by younghokim on 16. 8. 31..
 */
class NumericUpDownPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_numeric, context, attrs) {

    override var value: Int
        get() = picker.value
        set(value) {
            picker.setValue(value, INumericUpDown.ChangeType.MANUAL)
        }


    val picker: NumericUpDown = findViewById(R.id.ui_value)

    init {
        picker.valueChanged += {
            sender, args ->
            onValueChanged(args.newValue)
        }
    }


    override fun focus() {
    }

    override fun getSerializedValue(): String? {
        return value.toString()
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = serialized.toInt()
            return true
        } catch(e: Exception) {
            return false
        }
    }
}