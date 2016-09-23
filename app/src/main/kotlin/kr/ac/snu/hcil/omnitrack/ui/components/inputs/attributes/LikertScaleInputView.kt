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

    override var value: Float
        get() = scalePicker.value
        set(value) {
            scalePicker.value = value
        }

    val scalePicker: LikertScalePicker

    override fun focus() {
    }

    init {
        scalePicker = findViewById(R.id.value) as LikertScalePicker

        scalePicker.valueChanged += {
            sender, new: Float ->
            valueChanged.invoke(this, new)
        }
    }

}