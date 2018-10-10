package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

/**
 * Created by Young-Ho Kim on 2016-09-06
 */
open class HorizontalLinearDrawableView : View {

    abstract class ADrawableAdapter {

        abstract val numDrawables: Int
        abstract fun getDrawable(position: Int): Drawable
    }

    var adapter: ADrawableAdapter? = null
        set(value) {
            field = value
            calculateIntrinsicSize()
            invalidate()
        }

    var overridenIntrinsicWidth: Int? = null
        set(value) {
            field = value
            calculateIntrinsicSize()
            requestLayout()
        }

    var overridenIntrinsicHeight: Int? = null
        set(value) {
            field = value
            calculateIntrinsicSize()
            requestLayout()
        }

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0

    private var intrinsicWidth: Int = 0
    private var intrinsicHeight: Int = 0

    private var useIntrinsicWidth: Boolean = true

    private var boundRect: Rect = Rect()
    private var cellWidth: Int = 0
    private var cellHeight: Int = 0

    protected val currentCellWidth: Int get() = cellWidth


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
    }

    fun calculateIntrinsicSize() {
        intrinsicWidth = 0
        intrinsicHeight = 0
        if (adapter != null) {
            for (i in 0 until adapter!!.numDrawables) {
                intrinsicWidth += if (overridenIntrinsicWidth != null) {
                    overridenIntrinsicWidth!!
                } else {
                    adapter?.getDrawable(i)?.intrinsicWidth ?: 0
                }
                intrinsicHeight = if (overridenIntrinsicHeight != null) {
                    overridenIntrinsicHeight!!
                } else Math.max(intrinsicHeight, adapter?.getDrawable(i)?.intrinsicHeight ?: 0)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        //val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        //val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val measuredWidth: Int
        val measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY) {

            measuredWidth = widthSize
            measuredHeight = widthSize / (adapter?.numDrawables ?: 1)
            useIntrinsicWidth = false
        } else if (widthMode == MeasureSpec.AT_MOST) {

            measuredWidth = Math.min(intrinsicWidth + paddingStart + paddingEnd, widthSize)
            measuredHeight = measuredWidth / (adapter?.numDrawables ?: 1)
            useIntrinsicWidth = true
        } else {
            measuredWidth = intrinsicWidth + paddingStart + paddingEnd
            measuredHeight = intrinsicHeight
            useIntrinsicWidth = true
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = if (overridenIntrinsicWidth != null) {
            overridenIntrinsicWidth!!
        } else {
            w / (adapter?.numDrawables ?: 1)
        }
        cellHeight =
                if (overridenIntrinsicHeight != null) {
                    overridenIntrinsicHeight!!
                } else {
                    h
                }
    }


    override fun onDraw(canvas: Canvas) {
        if (adapter != null) {
            for (i in 0 until adapter!!.numDrawables) {
                val drawable = adapter!!.getDrawable(i)
                boundRect.set(0, 0, cellWidth, cellHeight)
                boundRect.offset(paddingLeft + i * cellWidth, paddingTop)
                drawable.bounds = boundRect
                drawable.draw(canvas)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            contentWidth = measuredWidth - paddingStart - paddingEnd
            contentHeight = measuredHeight - paddingTop - paddingBottom
        }
    }
}