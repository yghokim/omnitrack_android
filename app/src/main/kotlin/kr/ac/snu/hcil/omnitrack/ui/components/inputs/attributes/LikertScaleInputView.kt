package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.LikertScalePicker

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class LikertScaleInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Float>(R.layout.input_likert, context, attrs) {
    override val typeId: Int = AAttributeInputView.VIEW_TYPE_RATING_LIKERT

    override var value: Float? = 0.0f
        set(value) {
            if (field != value) {
                field = value

                scalePicker.valueChanged.suspend = true
                scalePicker.value = value ?: 0f
                scalePicker.valueChanged.suspend = false
            }
        }

    val scalePicker: LikertScalePicker = findViewById(R.id.value)

    override fun focus() {
    }

    init {
        scalePicker.valueChanged += {
            sender, new: Float ->
            this.value = new
            onValueChanged(new)
        }
    }

}