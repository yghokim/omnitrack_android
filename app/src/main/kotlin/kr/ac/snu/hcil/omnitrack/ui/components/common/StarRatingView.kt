package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-09-06.
 */
class StarRatingView : HorizontalLinearDrawableView {

    private var dragging = false

    var levels: Int by Delegates.observable(5) {
        prop, old, new ->
        if (old != new) {
            requestLayout()
            refresh()
        }
    }

    var allowIntermediate: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (old != new) {
            refresh()
        }
    }

    var score: Float by Delegates.observable(2.5f) {
        prop, old, new ->
        if (old != new) {
            refresh()
            scoreChanged.invoke(this, new)
        }
    }

    private val starAdapter: ADrawableAdapter = StarAdapter()

    val scoreChanged = Event<Float>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        adapter = starAdapter
    }

    private fun refresh() {
        invalidate()
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
        } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (adapter != null) {

            val adapter = adapter!!

            if (event.action == MotionEvent.ACTION_DOWN) {
                dragging = true

                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {

                if (event.x < paddingLeft) {
                    score = 0f
                } else if (event.x > paddingLeft + currentCellWidth * adapter.numDrawables) {
                    score = levels.toFloat()
                } else {
                    for (i in 0..adapter.numDrawables - 1) {
                        val left = paddingLeft + i * currentCellWidth
                        val right = left + currentCellWidth

                        if (left <= event.x && right >= event.x) {
                            val fraction = (event.x - left).toFloat() / currentCellWidth
                            score = i + discreteFraction(fraction)
                            break
                        }
                    }
                }

                return true
            } else if (event.action == MotionEvent.ACTION_UP) {
                dragging = false
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun discreteFraction(fraction: Float): Float {
        if (allowIntermediate) {

            if (fraction < 0.75f && fraction >= 0.25f) {
                return 0.5f
            } else if (fraction >= 0.75f) {
                return 1f
            } else return 0f
        } else {
            if (fraction >= 0.5f) return 1f
            else return 0f
        }
    }

    inner class StarAdapter : ADrawableAdapter() {
        override val numDrawables: Int get() = levels

        private val emptyDrawable: Drawable
        private val halfDrawable: Drawable
        private val fullDrawable: Drawable

        init {
            emptyDrawable = resources.getDrawable(R.drawable.rating_star_empty, null)
            halfDrawable = resources.getDrawable(R.drawable.rating_star_half, null)
            fullDrawable = resources.getDrawable(R.drawable.rating_star_full, null)
        }

        override fun getDrawable(position: Int): Drawable {
            if (score.toInt() > position) {
                return fullDrawable
            } else if (score.toInt() < position) {
                return emptyDrawable
            } else {
                val fraction = score - score.toInt()
                return when (discreteFraction(fraction)) {
                    0f -> emptyDrawable
                    0.5f -> halfDrawable
                    1f -> fullDrawable
                    else -> emptyDrawable
                }
            }
        }

    }

}