package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-09-06.
 */
class StarRatingView : HorizontalLinearDrawableView, GestureDetector.OnGestureListener {

    private val moveAmount: PointF = PointF()

    private val touchSlop = 30

    private var touchDownX = 0f

    private val gestureDetector: GestureDetector

    var isLightMode: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new) {
            refresh()
        }
    }

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

        gestureDetector = GestureDetector(context, this)

    }

    private fun refresh() {
        invalidate()
    }

/*
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        if (!isLightMode) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                parent.requestDisallowInterceptTouchEvent(true)
            } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }*/


    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (adapter != null && !isLightMode) {

            if (event.action == MotionEvent.ACTION_DOWN) {
                touchDownX = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {

                if (dragging) {
                    handleTouchEvent(event)
                } else {
                    val x = event.x
                    if (Math.abs(x - touchDownX) > touchSlop) {
                        dragging = true
                        if (!isLightMode) {
                            parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }

                return true
            } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                dragging = false
                moveAmount.x = 0f
                moveAmount.y = 0f
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun handleTouchEvent(event: MotionEvent) {
        val adapter = adapter!!
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
    }


    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        handleTouchEvent(p0)
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
    }

    override fun onLongPress(p0: MotionEvent?) {
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

        private val emptyDrawable: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.rating_star_empty)
        }
        private val halfDrawable: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.rating_star_half)
        }
        private val fullDrawable: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.rating_star_full)
        }

        private val emptyDrawableSmall: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.symbol_star_empty)
        }
        private val halfDrawableSmall: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.symbol_star_half)
        }
        private val fullDrawableSmall: Drawable by lazy {
            ContextCompat.getDrawable(context, R.drawable.symbol_star_full)
        }

        init {
        }

        override fun getDrawable(position: Int): Drawable {
            if (score.toInt() > position) {
                return if (!isLightMode) {
                    fullDrawable
                } else fullDrawableSmall
            } else if (score.toInt() < position) {
                return if (!isLightMode) {
                    emptyDrawable
                } else emptyDrawableSmall
            } else {
                val fraction = score - score.toInt()
                if (!isLightMode) {
                    return when (discreteFraction(fraction)) {
                        0f -> emptyDrawable
                        0.5f -> halfDrawable
                        1f -> fullDrawable
                        else -> emptyDrawable
                    }
                } else {
                    return when (discreteFraction(fraction)) {
                        0f -> emptyDrawableSmall
                        0.5f -> halfDrawableSmall
                        1f -> fullDrawableSmall
                        else -> emptyDrawableSmall
                    }
                }
            }
        }

    }

}