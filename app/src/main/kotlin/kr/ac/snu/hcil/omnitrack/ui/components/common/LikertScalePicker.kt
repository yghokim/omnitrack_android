package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.Fraction
import kr.ac.snu.hcil.omnitrack.utils.dipSize
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class LikertScalePicker : View, GestureDetector.OnGestureListener {

    var leftMost: Int by Delegates.observable(1) {
        prop, old, new ->
        if (old != new) {
            refreshVariableSizes()
            invalidate()
        }
    }

    var rightMost: Int by Delegates.observable(5) {
        prop, old, new ->
        if (old != new) {
            refreshVariableSizes()
            invalidate()
        }
    }


    var leftLabel: String by Delegates.observable(OTApp.instance.resourcesWrapped.getString(R.string.property_rating_options_leftmost_label_example)) {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var middleLabel: String by Delegates.observable("") {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var rightLabel: String by Delegates.observable(OTApp.instance.resourcesWrapped.getString(R.string.property_rating_options_rightmost_label_example)) {
        prop, old, new ->
        if (old != new) {
            requestLayout()
        }
    }

    var allowIntermediate: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (old != new) {
            invalidate()
        }
    }

    var value: Float? by Delegates.observable(((rightMost + leftMost) shr 1).toFloat() as Float?) {
        prop, old, new ->
        if (old != new) {
            invalidate()
            valueChanged.invoke(this, new)
        }
    }

    var fractionValue: Fraction?
        get() {
            val currentValue = value
            if (currentValue == null) {
                return null
            } else {
                val under = if (allowIntermediate) {
                    (rightMost - leftMost) * 10
                } else {
                    rightMost - leftMost
                }
                var upper = currentValue - leftMost
                if (allowIntermediate) upper *= 10

                return Fraction(Math.round(upper).toShort(), under.toShort())
            }
        }
        set(fraction) {
            if (fraction == null) {
                value = null
            } else {
                val under = if (allowIntermediate) {
                    (rightMost - leftMost) * 10
                } else {
                    rightMost - leftMost
                }
                var upper = Math.round((fraction.toFloat() * under))
                if (fraction.upper > 0) {
                    upper = Math.max(upper, 1)
                }

                value = (upper.toFloat() / under) * (rightMost - leftMost) + leftMost
            }
        }

    val valueChanged = Event<Float?>()

    val numPoints: Int get() = Math.abs(rightMost - leftMost) + 1

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0

    private var intrinsicHeight: Int = 0

    private var _pointDistance: Float = 0f
    private var _lineY: Float = 0f
    private var _valueY: Float = 0f
    private var _numberY: Float = 0f
    private var _labelY: Float = 0f
    private var _lineLeft: Float = 0f
    private var _lineRight: Float = 0f


    private val valueBoxVerticalPadding: Float
    private val valueBoxHorizontalPadding: Float
    private val valueBoxSpacing: Float
    private val valueIndicatorRadius: Float
    private val valueTextSize: Float
    private val pointRadius: Float
    private val numberSpacing: Float
    private val numberTextSize: Float
    private val labelTextSize: Float
    private val labelSpacing: Float

    private val labelTextPaint: TextPaint
    private val numberTextPaint: TextPaint
    private val valueTextPaint: TextPaint
    private val valueBoxPaint: Paint
    private val pointPaint: Paint
    private val linePaint: Paint
    private val valueIndicatorPaint: Paint

    private val boundRect = Rect()
    private val boxRect = RectF()

    private val touchSlop = 30

    private var touchDownX = 0f

    private var isDragging = false

    private val gestureDetector: GestureDetector

    init {
        valueBoxHorizontalPadding = resources.getDimension(R.dimen.likert_scale_value_box_padding_horizontal)
        valueBoxVerticalPadding = resources.getDimension(R.dimen.likert_scale_value_box_padding_vertical)
        valueBoxSpacing = resources.getDimension(R.dimen.likert_scale_value_box_spacing)
        valueIndicatorRadius = resources.getDimension(R.dimen.likert_scale_value_indicator_radius)
        valueTextSize = resources.getDimension(R.dimen.likert_scale_value_textSize)
        pointRadius = resources.getDimension(R.dimen.likert_scale_point_radius)
        numberSpacing = resources.getDimension(R.dimen.likert_scale_number_spacing)
        numberTextSize = resources.getDimension(R.dimen.likert_scale_number_textSize)
        labelTextSize = resources.getDimension(R.dimen.likert_scale_label_textSize)
        labelSpacing = resources.getDimension(R.dimen.likert_scale_label_spacing)

        labelTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        valueTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        valueBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        valueIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        numberTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = resources.getDimension(R.dimen.likert_scale_line_stroke_width)
        linePaint.color = ContextCompat.getColor(context, R.color.textColorLight)
        linePaint.alpha = 200

        pointPaint.style = Paint.Style.FILL
        pointPaint.color = ContextCompat.getColor(context, R.color.textColorLight)

        numberTextPaint.style = Paint.Style.FILL
        numberTextPaint.color = ContextCompat.getColor(context, R.color.textColorMidLight)
        numberTextPaint.textAlign = Paint.Align.CENTER
        numberTextPaint.textSize = numberTextSize
        numberTextPaint.isFakeBoldText = true

        valueIndicatorPaint.style = Paint.Style.FILL
        valueIndicatorPaint.color = ContextCompat.getColor(context, R.color.colorSecondary)

        valueTextPaint.style = Paint.Style.FILL
        valueTextPaint.textSize = valueTextSize
        valueTextPaint.color = Color.WHITE
        valueTextPaint.textAlign = Paint.Align.CENTER

        valueBoxPaint.style = Paint.Style.FILL
        valueBoxPaint.color = ContextCompat.getColor(context, R.color.colorSecondary)

        labelTextPaint.style = Paint.Style.FILL
        labelTextPaint.textSize = labelTextSize
        labelTextPaint.isFakeBoldText = true
        labelTextPaint.color = ContextCompat.getColor(context, R.color.textColorMid)

        gestureDetector = GestureDetector(context, this)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)


    private fun makeMultilineStaticLayout(text: String, paint: TextPaint, maxWidth: Int, align: Layout.Alignment): StaticLayout {
        return StaticLayout(text, paint, maxWidth.toInt(), align, 1f, 1f, false)
    }

    private fun textHeight(text: String, paint: Paint): Int {
        paint.getTextBounds(text, 0, text.length, boundRect)
        return boundRect.height()
    }

    private fun textHeight(text: String, paint: TextPaint, maxWidth: Int): Int {
        if (text.isNullOrBlank()) {
            return 0
        } else {
            val dl = makeMultilineStaticLayout(text, paint, maxWidth, Layout.Alignment.ALIGN_CENTER)
            println("text: text length: ${text.length}, lineCount: ${dl.lineCount}, height: ${dl.height}")
            return dl.height
        }
    }

    private fun textWidth(text: String, paint: Paint): Int {
        paint.getTextBounds(text, 0, text.length, boundRect)
        return boundRect.width()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(_lineLeft, _lineY, _lineRight, _lineY, linePaint)

        for (i in 0..numPoints - 1) {
            val centerX = _lineLeft + _pointDistance * i
            val numberText = (i + leftMost).toString()

            canvas.drawCircle(centerX, _lineY, pointRadius, pointPaint)

            val numberX: Float = getWrappedCenterPoint(centerX, textWidth(numberText, numberTextPaint) / 2f)

            canvas.drawText(numberText, numberX, _lineY + numberTextSize + numberSpacing, numberTextPaint)
        }


        //draw labels

        labelTextPaint.textAlign = Paint.Align.LEFT
        val leftLabelLayout = makeMultilineStaticLayout(leftLabel, labelTextPaint, contentWidth / 5, Layout.Alignment.ALIGN_NORMAL)
        canvas.save()

        canvas.translate(paddingLeft.toFloat(), _labelY)
        leftLabelLayout.draw(canvas)
        canvas.restore()


        labelTextPaint.textAlign = Paint.Align.LEFT
        val middleLabelLayout = makeMultilineStaticLayout(middleLabel, labelTextPaint, contentWidth / 5, Layout.Alignment.ALIGN_CENTER)
        canvas.save()

        canvas.translate(((_lineLeft + _lineRight) / 2 - (middleLabelLayout.width shr 1)).toFloat(), _labelY)
        middleLabelLayout.draw(canvas)
        canvas.restore()


        labelTextPaint.textAlign = Paint.Align.RIGHT
        val rightLabelLayout = makeMultilineStaticLayout(rightLabel, labelTextPaint, contentWidth / 5, Layout.Alignment.ALIGN_NORMAL)
        canvas.save()

        canvas.translate((paddingLeft + contentWidth).toFloat() - dipSize(2), _labelY)
        rightLabelLayout.draw(canvas)
        canvas.restore()


        //draw value
        val value = value
        if (value != null) {
            val valuePosition = convertValueToCoordinate(value)

            canvas.drawCircle(valuePosition, _lineY, valueIndicatorRadius, valueIndicatorPaint)

            val valueText = value.toString()
            valueTextPaint.getTextBounds(valueText, 0, valueText.length, boundRect)
            val valueCenter = getWrappedCenterPoint(valuePosition, boundRect.width() / 2 + valueBoxHorizontalPadding)
            boxRect.set(valueCenter - boundRect.width() / 2 - valueBoxHorizontalPadding, 0f, valueCenter + boundRect.width() / 2 + valueBoxHorizontalPadding, valueTextSize + 2 * valueBoxVerticalPadding)
            canvas.drawRoundRect(boxRect, 15f, 15f, valueBoxPaint)

            canvas.drawText(valueText, valueCenter, (boxRect.top + boxRect.bottom) / 2 + boundRect.height() / 2, valueTextPaint)
        }
    }

    fun getWrappedCenterPoint(desired: Float, contentHalfWidth: Float): Float {
        val left = paddingLeft
        val right = paddingLeft + contentWidth

        return Math.min(Math.max(
                desired,
                contentHalfWidth + left
        ),
                right - contentHalfWidth
        )
    }

    fun getSnappedValue(original: Float): Float {
        return if (allowIntermediate) {
            (original * 10 + .5f).toInt() / 10f
        } else {
            Math.round(original).toFloat()
        }
    }

    fun convertValueToCoordinate(value: Float): Float {
        return (_lineRight - _lineLeft) * ((getSnappedValue(value) - leftMost) / (rightMost - leftMost)) + _lineLeft
    }

    fun convertCoordinateToValue(x: Float): Float {
        return getSnappedValue((rightMost - leftMost) * ((x - _lineLeft) / (_lineRight - _lineLeft)) + leftMost)
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

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
    }

    override fun onLongPress(p0: MotionEvent?) {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            touchDownX = event.x
            return true
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (isDragging) {
                handleTouchEvent(event)
            } else {
                val x = event.x
                if (Math.abs(x - touchDownX) > touchSlop) {
                    isDragging = true
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            return true
        } else if (event.action == MotionEvent.ACTION_UP) {
            isDragging = false
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun handleTouchEvent(event: MotionEvent) {
        if (event.x < _lineLeft) {
            value = leftMost.toFloat()
        } else if (event.x > _lineRight) {
            value = rightMost.toFloat()
        } else {
            value = getSnappedValue(convertCoordinateToValue(event.x))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val measuredWidth: Int
        val measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = widthSize
        } else {
            measuredWidth = 400 + paddingLeft + paddingRight
        }

        contentWidth = measuredWidth - paddingStart - paddingEnd
        intrinsicHeight = (2 * valueBoxVerticalPadding + valueTextSize + valueBoxSpacing + numberSpacing + numberTextSize + labelSpacing +
                Math.max(Math.max(textHeight(leftLabel, labelTextPaint, contentWidth / 5), textHeight(rightLabel, labelTextPaint, contentWidth / 5)), textHeight(middleLabel, labelTextPaint, contentWidth / 5)) + 0.5f).toInt()

        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(intrinsicHeight + paddingTop + paddingBottom, heightSize).toInt()
        } else {
            measuredHeight = intrinsicHeight.toInt() + paddingTop + paddingBottom
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun refreshVariableSizes() {

        _valueY = valueBoxVerticalPadding + valueTextSize
        _lineY = _valueY + valueBoxVerticalPadding + valueBoxSpacing
        _numberY = _lineY + numberSpacing
        _labelY = _numberY + labelSpacing + numberTextSize

        _lineLeft = paddingLeft + Math.max(pointRadius, valueIndicatorRadius)
        _lineRight = paddingLeft + contentWidth - Math.max(pointRadius, valueIndicatorRadius)


        _pointDistance = if (numPoints > 1) {
            (_lineRight - _lineLeft) / (numPoints - 1)
        } else {
            0f
        }

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            contentWidth = measuredWidth - paddingStart - paddingEnd
            contentHeight = measuredHeight - paddingTop - paddingBottom

            refreshVariableSizes()
        }
    }
}