package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2017. 4. 15..
 */
class StarScoreView : LinearLayout {

    companion object {
        val scoreTypeSpan: TypefaceSpan by lazy { TypefaceSpan("sans-serif") }
        val scoreSizeSpan: RelativeSizeSpan by lazy { RelativeSizeSpan(1.5f) }
    }

    private val scoreColorSpan: ForegroundColorSpan by lazy { ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorMid)) }

    private val scoreView: TextView

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        orientation = HORIZONTAL
        inflateContent(R.layout.component_star_score_view, true)
        scoreView = findViewById(R.id.ui_text)
    }

    fun setScore(score: Float, maxScore: Float) {
        val maxScoreString = if (maxScore - maxScore.toInt() < 0.0000001) {
            maxScore.toInt().toString()
        } else maxScore.toString()

        val scoreString = if (score - score.toInt() < 0.000000001) {
            score.toInt().toString()
        } else score.toString()

        val spannable = SpannableString("$scoreString/$maxScoreString").apply {
            this.setSpan(scoreTypeSpan, 0, scoreString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.setSpan(scoreSizeSpan, 0, scoreString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.setSpan(scoreColorSpan, 0, scoreString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        scoreView.text = spannable
    }

}