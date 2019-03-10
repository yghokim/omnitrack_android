package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.types.Fraction
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarRatingSlider

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class StarRatingInputView(context: Context, attrs: AttributeSet? = null, initialValue: Fraction? = null) : AAttributeInputView<Fraction>(R.layout.input_stars, context, attrs) {
    override val typeId: Int = VIEW_TYPE_RATING_STARS

    val ratingView: StarRatingSlider = findViewById(R.id.ui_value)

    override var value: Fraction? = initialValue
        set(value) {
            if (field != value) {
                field = value
                ratingView.scoreChanged.suspend = true
                ratingView.fractionValue = value
                ratingView.scoreChanged.suspend = false
                onValueChanged(value)
            }
        }

    init {
        ratingView.fractionValue = initialValue
        ratingView.scoreChanged += { sender, score: Float? ->
            this.value = ratingView.fractionValue
        }
    }

    override fun focus() {
        ratingView.requestFocus()
    }
}