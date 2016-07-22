package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTAttribute

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class NumberInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Float>(R.layout.input_number, context, attrs) {
    override val typeId: Int = TYPE_NUMBER
    override var value: Float
        get() = throw UnsupportedOperationException()
        set(value) {
        }

    override fun focus() {

    }

}