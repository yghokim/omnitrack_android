package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kr.ac.snu.hcil.android.common.dipSize
import kr.ac.snu.hcil.omnitrack.R
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingButton : AppCompatButton, ValueAnimator.AnimatorUpdateListener {

    var state: AudioRecorderView.State by Delegates.observable(AudioRecorderView.State.RECORD) {
        prop, old, new ->
        if (old != new) {

            when (new) {
                AudioRecorderView.State.RECORD -> {
                    radiusAnimator.cancel()
                    colorAnimator.cancel()
                    currentCircleRadius = buttonMaxRadius - circleInset
                    currentCircleColor = circleNormalColor
                }

                AudioRecorderView.State.RECORDING -> {
                    radiusAnimator.start()
                    colorAnimator.start()
                }

                AudioRecorderView.State.FILE_MOUNTED -> {
                    colorAnimator.reverse()
                    radiusAnimator.cancel()

                    currentCircleRadius = buttonMaxRadius
                    currentCircleColor = circleNormalColor
                }
            }

            invalidate()
        }
    }

    private val frameCirclePaint: Paint

    private val buttonCirclePaint: Paint

    private val circleNormalColor: Int
    private val circlePressedColor: Int
    private val circleStopColor: Int

    private var buttonMaxRadius: Float = 0f
    private var buttonCenterX: Float = 0f
    private var buttonCenterY: Float = 0f

    private val circleInset: Float

    private val drawableRatio: Float = 0.6f

    private val colorAnimator: ValueAnimator
    private val radiusAnimator: ValueAnimator

    private var currentCircleColor: Int
    private var currentCircleRadius: Float


    private val stopDrawable: Drawable by lazy {
        val drawable = ContextCompat.getDrawable(context, R.drawable.stop_dark)!!
        drawable.setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP)
        drawable.alpha = 150
        drawable
    }

    private val removeDrawable: Drawable by lazy {
        val drawable = ContextCompat.getDrawable(context, R.drawable.trashcan)!!
        drawable.setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP)
        drawable.alpha = 200
        drawable
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        frameCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        frameCirclePaint.style = Paint.Style.STROKE
        frameCirclePaint.color = ContextCompat.getColor(context, R.color.dividerColor)
        frameCirclePaint.strokeWidth = dipSize(context, 1)

        buttonCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        buttonCirclePaint.style = Paint.Style.FILL
        circleNormalColor = ContextCompat.getColor(context, R.color.colorRed)
        circlePressedColor = ColorUtils.blendARGB(circleNormalColor, Color.BLACK, 0.2f)
        circleStopColor = ContextCompat.getColor(context, R.color.colorPointed)

        circleInset = resources.getDimension(R.dimen.audio_recorder_circle_inset)


        colorAnimator = ValueAnimator.ofInt(circleNormalColor, circleStopColor).setDuration(600)
        colorAnimator.setEvaluator(ArgbEvaluator())
        colorAnimator.addUpdateListener(this)

        radiusAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).setDuration(1000)
        radiusAnimator.repeatCount = ValueAnimator.INFINITE
        radiusAnimator.addUpdateListener(this)

        background = null
        setPadding(0, 0, 0, 0)

        currentCircleColor = circleNormalColor
        currentCircleRadius = buttonMaxRadius - circleInset
    }

    override fun onDraw(canvas: Canvas) {
        //super.onDraw(canvas)

        //draw frame
        canvas.drawCircle(buttonCenterX, buttonCenterY, buttonMaxRadius - frameCirclePaint.strokeWidth, frameCirclePaint)


        //draw circle
        buttonCirclePaint.color = currentCircleColor

        canvas.drawCircle(buttonCenterX, buttonCenterY, currentCircleRadius, buttonCirclePaint)

        if (state != AudioRecorderView.State.RECORD) {
            val drawableRadius = buttonMaxRadius * drawableRatio
            val drawable = if (state == AudioRecorderView.State.RECORDING) {
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

            if (state == AudioRecorderView.State.RECORD) {
                currentCircleRadius = buttonMaxRadius - circleInset
            } else if (state == AudioRecorderView.State.FILE_MOUNTED) {
                currentCircleRadius = buttonMaxRadius
            }
        }
    }


    override fun onAnimationUpdate(animator: ValueAnimator) {

        if (animator === colorAnimator) {
            currentCircleColor = colorAnimator.animatedValue as Int
        } else if (animator === radiusAnimator) {
            currentCircleRadius = (buttonMaxRadius - circleInset) + circleInset * radiusAnimator.animatedValue as Float
        }

        invalidate()
    }

}