package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.Fraction
import kr.ac.snu.hcil.omnitrack.ui.components.common.LikertScalePicker

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class LikertScaleInputView(context: Context, attrs: AttributeSet? = null, initialValue: Fraction? = null) : AAttributeInputView<Fraction>(R.layout.input_likert, context, attrs) {
    override val typeId: Int = AAttributeInputView.VIEW_TYPE_RATING_LIKERT

    override var value: Fraction? = initialValue
        set(value) {
            if (field != value) {
                field = value

                scalePicker.valueChanged.suspend = true
                scalePicker.fractionValue = value
                scalePicker.valueChanged.suspend = false

                onValueChanged(value)
            }
        }

    val scalePicker: LikertScalePicker = findViewById(R.id.ui_value)

    override fun focus() {
    }

    init {
        scalePicker.value = initialValue?.toFloat()
        scalePicker.valueChanged += { sender, new: Float? ->
            this.value = scalePicker.fractionValue
        }
    }

}