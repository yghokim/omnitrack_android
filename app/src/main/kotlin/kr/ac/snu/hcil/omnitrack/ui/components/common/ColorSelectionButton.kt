package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import kr.ac.snu.hcil.omnitrack.R

class ColorSelectionButton : AppCompatButton, ValueAnimator.AnimatorUpdateListener {

    var color: Int = Color.RED
        set(value) {
            if (field != value) {
                field = value
                shapePaint.color = value
                invalidate()
            }
        }

    lateinit var checkedDrawable: Drawable

    private val frameBounds = RectF()
    private var paddingTop = 0.0f
    private var paddingLeft = 0.0f

    private var contentSize: Float = 0.0f

    private var cornerRadius: Float = 0.0f
    private var contentPadding: Float = 0.0f


    val shapePaint: Paint = Paint()

    private var toSelectionRoundingAnimator: ValueAnimator? = null
    private var toUnselectionRoundingAnimator: ValueAnimator? = null

    private var toSelectionRadiusAnimator: ValueAnimator? = null
    private var toUnselectionRadiusAnimator: ValueAnimator? = null


    companion object {
        val interpolator = DecelerateInterpolator(2.0f)
    }


    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        background = null
        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.ColorSelectionButton, defStyle, 0)

        color = a.getColor(R.styleable.ColorSelectionButton_buttonColor, Color.RED)

        a.recycle()

        // Set up a default TextPaint object
        shapePaint.flags = Paint.ANTI_ALIAS_FLAG

        checkedDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.done, null)!!
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {

            contentSize = Math.min(width, height).toFloat()
            if (width >= height) {
                paddingTop = 0.0f
                paddingLeft = (width - contentSize) * .5f
            } else {
                paddingTop = (height - contentSize) * .5f
                paddingLeft = 0.0f
            }

            frameBounds.set(paddingLeft, paddingTop, paddingLeft + contentSize, paddingTop + contentSize)
            val paddingSize = Math.round(contentSize * 0.07f)
            val drawableSize = Math.round(paddingSize + contentSize - 2 * paddingSize)
            checkedDrawable.setBounds(Math.round(paddingLeft + paddingSize), Math.round(paddingTop + paddingSize), Math.round(paddingLeft + drawableSize), Math.round(paddingTop + drawableSize))

            if (isSelected) {
                cornerRadius = contentSize * .2f
                contentPadding = 0.0f
            } else {
                cornerRadius = contentSize * .5f
                contentPadding = contentSize * 0.1f
            }

            toSelectionRoundingAnimator = ValueAnimator.ofFloat(contentSize * 0.5f, contentSize * 0.2f)
            toUnselectionRoundingAnimator = ValueAnimator.ofFloat(contentSize * 0.2f, contentSize * 0.5f)

            toSelectionRadiusAnimator = ValueAnimator.ofFloat(contentSize * 0.1f, 0.0f)
            toUnselectionRadiusAnimator = ValueAnimator.ofFloat(0.0f, contentSize * 0.1f)


            toSelectionRoundingAnimator?.interpolator = interpolator
            toUnselectionRoundingAnimator?.interpolator = interpolator

            toSelectionRoundingAnimator?.duration = 200
            toSelectionRadiusAnimator?.duration = 200
            toUnselectionRoundingAnimator?.duration = 150
            toUnselectionRadiusAnimator?.duration = 150

            toSelectionRoundingAnimator?.addUpdateListener(this)
            toUnselectionRoundingAnimator?.addUpdateListener(this)

            toSelectionRadiusAnimator?.addUpdateListener(this)
            toUnselectionRadiusAnimator?.addUpdateListener(this)
        }
    }

    override fun setSelected(selected: Boolean) {
        if (isSelected != selected) {
            if (selected) {
                if (toUnselectionRoundingAnimator?.isRunning == true) {
                    toUnselectionRoundingAnimator?.end()
                }

                toSelectionRadiusAnimator?.start()
                toSelectionRoundingAnimator?.start()
            } else {
                if (toSelectionRoundingAnimator?.isRunning == true) {
                    toSelectionRoundingAnimator?.end()
                }

                toUnselectionRadiusAnimator?.start()
                toUnselectionRoundingAnimator?.start()
            }
        }
        super.setSelected(selected)
    }

    override fun onAnimationUpdate(p0: ValueAnimator) {
        if (p0 === toUnselectionRoundingAnimator || p0 === toSelectionRoundingAnimator) {
            cornerRadius = p0.animatedValue as Float
        } else {
            contentPadding = p0.animatedValue as Float
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        frameBounds.set(paddingLeft, paddingTop, paddingLeft + contentSize, paddingTop + contentSize)
        frameBounds.inset(contentPadding, contentPadding)
        canvas.drawRoundRect(frameBounds, cornerRadius, cornerRadius, shapePaint)

        if (isSelected) {
            checkedDrawable.draw(canvas)

        }
    }
}
