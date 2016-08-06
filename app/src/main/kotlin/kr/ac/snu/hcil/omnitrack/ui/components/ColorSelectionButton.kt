package kr.ac.snu.hcil.omnitrack.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import kr.ac.snu.hcil.omnitrack.R

/**
 * TODO: document your custom view class.
 */
class ColorSelectionButton : Button, ValueAnimator.AnimatorUpdateListener {

    var color:Int = Color.RED // TODO: use a default from R.color...
        set(value){
            if(field != value) {
                field = value
                shapePaint.color = value
                invalidate()
            }
        }

    lateinit var checkedDrawable: Drawable

    private val frameBounds = RectF()
    private var paddingTop = 0.0f
    private var paddingLeft = 0.0f

    private var contentSize : Float = 0.0f

    private var cornerRadius: Float = 0.0f

    val shapePaint : Paint = Paint()

    private var toSelectionAnimator: ValueAnimator? = null
    private var toUnselectionAnimator: ValueAnimator? = null

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

        checkedDrawable = resources.getDrawable(R.drawable.done, null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if(changed == true)
        {

            contentSize = Math.min(width, height).toFloat()
            if(width >= height)
            {
                paddingTop = 0.0f
                paddingLeft = (width - contentSize)*.5f
            }
            else{
                paddingTop = (height - contentSize)*.5f
                paddingLeft = 0.0f
            }

            frameBounds.set(paddingLeft, paddingTop, paddingLeft + contentSize, paddingTop + contentSize)
            val paddingSize = Math.round(contentSize * 0.07f)
            val drawableSize = Math.round(paddingSize + contentSize - 2*paddingSize)
            checkedDrawable.setBounds(Math.round(paddingLeft + paddingSize), Math.round(paddingTop + paddingSize), drawableSize, drawableSize)

            if (isSelected) {
                cornerRadius = contentSize * .2f
            } else {
                cornerRadius = contentSize * .5f
            }

            toSelectionAnimator = ValueAnimator.ofFloat(contentSize * 0.5f, contentSize * 0.2f)
            toUnselectionAnimator = ValueAnimator.ofFloat(contentSize * 0.2f, contentSize * 0.5f)

            toSelectionAnimator?.interpolator = interpolator
            toUnselectionAnimator?.interpolator = interpolator

            toSelectionAnimator?.duration = 200
            toUnselectionAnimator?.duration = 150

            toSelectionAnimator?.addUpdateListener(this)
            toUnselectionAnimator?.addUpdateListener(this)
        }
    }

    override fun setSelected(selected: Boolean) {
        if (isSelected != selected) {
            if (selected) {
                if (toUnselectionAnimator?.isRunning ?: false) {
                    toUnselectionAnimator?.end()
                }

                toSelectionAnimator?.start()
            } else {
                if (toSelectionAnimator?.isRunning ?: false) {
                    toSelectionAnimator?.end()
                }

                toUnselectionAnimator?.start()
            }
        }
        super.setSelected(selected)
    }

    override fun onAnimationUpdate(p0: ValueAnimator) {
        cornerRadius = p0.animatedValue as Float
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(frameBounds, cornerRadius, cornerRadius, shapePaint)


        if (isSelected)
        {
            checkedDrawable.draw(canvas)

        }
    }
}
