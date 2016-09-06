package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.StarRatingView

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class StarRatingInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Float>(R.layout.input_stars, context, attrs) {
    override val typeId: Int = VIEW_TYPE_RATING_STARS

    val ratingView: StarRatingView

    override var value: Float
        get() = ratingView.score
        set(value) {
            ratingView.score = value
        }

    init {
        ratingView = findViewById(R.id.value) as StarRatingView

        ratingView.scoreChanged += {
            sender, score: Float ->
            valueChanged.invoke(this, score)
        }
    }

    override fun focus() {
        ratingView.requestFocus()
    }
}