package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.NumericUpDown

/**
 * Created by younghokim on 16. 8. 31..
 */
class NumericUpDownPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<Int>(R.layout.component_property_numeric, context, attrs) {

    override var value: Int
        get() = picker.value
        set(value) {
            picker.value = value
        }


    val picker: NumericUpDown

    init {
        picker = findViewById(R.id.value) as NumericUpDown
        picker.valueChanged += {
            sender, args ->
            onValueChanged(args)
        }
    }


    override fun focus() {
    }
}