package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.ToggleButton

import kr.ac.snu.hcil.omnitrack.R

/**
 * TODO: document your custom view class.
 */
class ColorSelectionButton : ToggleButton {
    var color:Int = Color.RED // TODO: use a default from R.color...
        set(value){
            if(field != value) {
                field = value
                shapePaint.color = value
                invalidate()
            }
        }

    /**
     * Gets the example drawable attribute value.

     * @return The example drawable attribute value.
     */
    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.

     * @param exampleDrawable The example drawable attribute value to use.
     */
    lateinit var checkedDrawable: Drawable

    private val frameBounds = RectF()
    private var paddingTop = 0.0f
    private var paddingLeft = 0.0f

    private var contentSize : Float = 0.0f

    val shapePaint : Paint = Paint()

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
        }
    }

    override fun onDraw(canvas: Canvas) {
        if(isChecked)
        {
            canvas.drawRoundRect(frameBounds, contentSize*0.2f, contentSize*0.2f, shapePaint)
            checkedDrawable.draw(canvas)

        }else{
            canvas.drawCircle(paddingLeft + contentSize*.5f, paddingTop + contentSize*.5f, contentSize*.5f, shapePaint)
        }

    }
}
