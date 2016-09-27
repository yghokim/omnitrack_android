package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.graphics.ColorUtils
import android.util.AttributeSet
import android.widget.Button
import kr.ac.snu.hcil.omnitrack.R
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingButton : Button {

    enum class State {
        RECORD, RECORDING, REMOVE
    }

    var state: State by Delegates.observable(State.RECORD) {
        prop, old, new ->
        if (old != new) {
            invalidate()
        }
    }

    private val frameCirclePaint: Paint

    private val buttonCirclePaint: Paint

    private val circleNormalColor: Int
    private val circlePressedColor: Int

    private var buttonMaxRadius: Float = 0f
    private var buttonCenterX: Float = 0f
    private var buttonCenterY: Float = 0f

    private val circleInset: Float

    private val drawableRatio: Float = 0.7f

    private val stopDrawable: Drawable by lazy {
        val drawable = resources.getDrawable(R.drawable.stop_dark, null)
        drawable.setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP)
        drawable.alpha = 150
        drawable
    }

    private val removeDrawable: Drawable by lazy {
        val drawable = resources.getDrawable(R.drawable.trashcan, null)
        drawable.setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP)
        drawable.alpha = 150
        drawable
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        frameCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        frameCirclePaint.style = Paint.Style.STROKE
        frameCirclePaint.color = resources.getColor(R.color.dividerColor, null)
        frameCirclePaint.strokeWidth = 1 * resources.displayMetrics.density

        buttonCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        buttonCirclePaint.style = Paint.Style.FILL
        circleNormalColor = resources.getColor(R.color.colorRed, null)
        circlePressedColor = ColorUtils.blendARGB(circleNormalColor, Color.BLACK, 0.8f)

        circleInset = resources.getDimension(R.dimen.audio_recorder_circle_inset)
        background = null
        setPadding(0, 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas) {
        //super.onDraw(canvas)

        //draw frame
        canvas.drawCircle(buttonCenterX, buttonCenterY, buttonMaxRadius - frameCirclePaint.strokeWidth, frameCirclePaint)


        //draw circle
        if (isPressed) {
            buttonCirclePaint.color = circlePressedColor
        } else {
            buttonCirclePaint.color = circleNormalColor
        }

        canvas.drawCircle(buttonCenterX, buttonCenterY, buttonMaxRadius - circleInset, buttonCirclePaint)


        if (state != State.RECORD) {
            val drawableRadius = buttonMaxRadius * drawableRatio
            val drawable = if (state == State.RECORDING) {
                stopDrawable
            } else {
                removeDrawable
            }

            drawable.setBounds(
                    (buttonCenterX - drawableRadius + .5f).toInt(),
                    (buttonCenterY - drawableRadius + .5f).toInt(),
                    (buttonCenterX + drawableRadius + .5f).toInt(),
                    (buttonCenterY + drawableRadius + .5f).toInt()
            )

            drawable.draw(canvas)
        }

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val width = right - left - paddingLeft - paddingRight
            val height = bottom - top - paddingTop - paddingBottom
            buttonCenterX = paddingLeft + width * .5f
            buttonCenterY = paddingTop + height * .5f
            buttonMaxRadius = Math.min(width, height) * .5f
        }
    }

}