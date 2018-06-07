package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.ColorUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import butterknife.bindView
import butterknife.bindViews
import kotlinx.android.synthetic.main.component_duration_picker.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper

/**
 * Created by younghokim on 16. 8. 23..
 */
class DurationPicker : ConstraintLayout, View.OnClickListener {

    companion object {
        const val MAX_DURATION_SECONDS = 99 * 3600 + 99 * 60 + 99
    }

    private val display: View by bindView(R.id.ui_digit_area)
    private val hourView: TextView by bindView(R.id.ui_digit_hour)
    private val minuteView: TextView by bindView(R.id.ui_digit_minute)
    private val secondView: TextView by bindView(R.id.ui_digit_second)
    private val backspaceButton: View by bindView(R.id.ui_button_backspace)

    private val digitButtons: List<View> by bindViews(
            R.id.ui_keypad_0, R.id.ui_keypad_1, R.id.ui_keypad_2, R.id.ui_keypad_3, R.id.ui_keypad_4, R.id.ui_keypad_5, R.id.ui_keypad_6, R.id.ui_keypad_7, R.id.ui_keypad_8, R.id.ui_keypad_9
    )
    private val digitButton00: View by bindView(R.id.ui_keypad_00)
    private val digitButtonUp: View by bindView(R.id.ui_keypad_up)

    private val buttons: List<View> by lazy {
        digitButtons + listOf<View>(
                digitButton00, digitButtonUp
        )
    }


    private val digits = Array<Byte>(6) { 0 }

    private var suspendRefreshDigits = false


    private var mIsInInputMode: Boolean = false

    val isInInputMode: Boolean get() = mIsInInputMode

    val onSecondsChanged = Event<Int>()

    var durationSeconds: Int = 0
        set(value) {

            if (field != value) {
                if (value > MAX_DURATION_SECONDS) {
                    throw IllegalArgumentException("duration seconds cannot exceed 99h 99m 99s.")
                }

                field = value

                if (!suspendRefreshDigits) {
                    var different = value * 1000


                    var elapsedHours = different / TimeHelper.hoursInMilli.toInt()
                    different %= TimeHelper.hoursInMilli.toInt()

                    var elapsedMinutes = different / TimeHelper.minutesInMilli.toInt()
                    different %= TimeHelper.minutesInMilli.toInt()

                    var elapsedSeconds = different / TimeHelper.secondsInMilli.toInt()

                    if (elapsedHours > 99) {
                        elapsedMinutes += (elapsedHours - 99) * 60
                        elapsedHours = 99
                    }

                    if (elapsedMinutes > 99) {
                        elapsedSeconds += (elapsedMinutes - 99) * 60
                        elapsedMinutes = 99
                    }

                    setDigitArray(elapsedHours, 0)
                    setDigitArray(elapsedMinutes, 2)
                    setDigitArray(elapsedSeconds, 4)

                    applyDigitsToView()
                }

                onSecondsChanged.invoke(this, value)
            }

        }

    private var displayHeight: Int = 0
    private var keyPadHeight: Int = 0
    private val highlightBackgroundColor: Int by lazy {
        ContextCompat.getColor(context, R.color.editTextFormBackground)
    }

    private val updateListener = ValueAnimator.AnimatorUpdateListener {
        animator ->

        val value = animator.animatedValue as Float

        this.backspaceButton.alpha = value
        buttons.forEach {
            it.alpha = value
        }

        this.setBackgroundColor(ColorUtils.setAlphaComponent(highlightBackgroundColor, (255 * value).toInt()))

        this.layoutParams.height = (displayHeight + (ui_keypad_1.layoutParams as ConstraintLayout.LayoutParams).topMargin + keyPadHeight * value + 0.5f).toInt()

        this.requestLayout()
    }

    private val openAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            duration = 350
            addUpdateListener(updateListener)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    backspaceButton.visibility = View.VISIBLE
                    ui_keypad_group.visibility = View.VISIBLE

                    buttons.forEach {
                        it.alpha = 1f
                    }

                    backspaceButton.alpha = 1f
                    requestLayout()
                }

                override fun onAnimationCancel(p0: Animator?) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    backspaceButton.visibility = View.VISIBLE
                    ui_keypad_group.visibility = View.VISIBLE

                    buttons.forEach {
                        it.alpha = 1f
                    }

                    backspaceButton.alpha = 1f
                    requestLayout()
                }

                override fun onAnimationStart(p0: Animator?) {
                    ui_keypad_group.visibility = View.VISIBLE
                    backspaceButton.visibility = View.VISIBLE
                }
            })
        }
    }

    private val closeAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(1f, 0f).apply {
            interpolator = DecelerateInterpolator()
            duration = 250
            addUpdateListener(updateListener)

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    backspaceButton.visibility = View.GONE
                    ui_keypad_group.visibility = View.GONE
                    buttons.forEach {
                        it.alpha = 1f
                    }
                    backspaceButton.alpha = 1f
                    background = null
                    requestLayout()
                }

                override fun onAnimationCancel(p0: Animator?) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    backspaceButton.visibility = View.GONE
                    ui_keypad_group.visibility = View.GONE
                    buttons.forEach {
                        it.alpha = 1f
                    }
                    backspaceButton.alpha = 1f
                    background = null
                    requestLayout()
                }

                override fun onAnimationStart(p0: Animator?) {
                    ui_keypad_group.visibility = View.VISIBLE
                }
            })
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflateContent(R.layout.component_duration_picker, true)

        display.setOnClickListener(this)

        backspaceButton.setOnClickListener(this)

        for (button in digitButtons)
            button.setOnClickListener(this)

        digitButton00.setOnClickListener(this)
        digitButtonUp.setOnClickListener(this)
    }

    fun setInputMode(mode: Boolean, animate: Boolean) {
        if (mIsInInputMode != mode) {
            mIsInInputMode = mode

            if (closeAnimator.isRunning) {
                closeAnimator.cancel()
            }

            if (openAnimator.isRunning) {
                openAnimator.cancel()
            }

            if (animate) {
                if (mode) {
                    openAnimator.start()
                } else {
                    closeAnimator.start()
                }
            } else {
                if (mode) {
                    setBackgroundColor(ResourcesCompat.getColor(resources, R.color.editTextFormBackground, null))
                    backspaceButton.visibility = View.VISIBLE
                    ui_keypad_group.visibility = View.VISIBLE
                } else {
                    background = null
                    backspaceButton.visibility = View.GONE
                    ui_keypad_group.visibility = View.GONE
                }
            }
        }
    }

    private fun applyDigitsToView() {
        hourView.text = digits[0].toString() + digits[1].toString()
        minuteView.text = digits[2].toString() + digits[3].toString()
        secondView.text = digits[4].toString() + digits[5].toString()
    }

    private fun setDigitArray(number: Int, indexStart: Int) {
        digits[indexStart] = (number / 10).toByte()
        digits[indexStart + 1] = (number % 10).toByte()
    }

    private fun getNumberFromDigitArray(indexStart: Int): Int {
        return digits[indexStart] * 10 + digits[indexStart + 1]
    }

    private fun updateDurationFromDigits() {
        suspendRefreshDigits = true

        durationSeconds = getNumberFromDigitArray(0) * 3600 + getNumberFromDigitArray(2) * 60 + getNumberFromDigitArray(4)

        suspendRefreshDigits = false
    }

    private fun shiftDigit(vararg newDigits: Byte) {
        for (i in newDigits.size..digits.size - 1) {
            digits[i - newDigits.size] = digits[i]
        }

        for (digit in newDigits.withIndex()) {
            digits[digits.size - newDigits.size + digit.index] = digit.value
        }

        updateDurationFromDigits()
        applyDigitsToView()
    }

    private fun unShiftDigit() {
        println("unshift")
        for (i in (0..digits.size - 2).reversed()) {
            digits[i + 1] = digits[i]
        }
        digits[0] = 0

        updateDurationFromDigits()
        applyDigitsToView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val activity = getActivity() as? OTActivity
        if (activity != null) {
            activity.durationPickers.add(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (openAnimator.isRunning) {
            openAnimator.cancel()
        }

        if (closeAnimator.isRunning) {
            closeAnimator.cancel()
        }

        val activity = getActivity() as? OTActivity
        if (activity != null) {
            activity.durationPickers.remove(this)
        }
    }


    override fun onClick(view: View) {
        if (view === display) {
            setInputMode(!isInInputMode, true)
        } else if (view === backspaceButton) {
            unShiftDigit()
        } else if (view === digitButton00) {
            shiftDigit(0, 0)
        } else if (view === digitButtonUp) {
            setInputMode(false, true)
        } else {
            val digit = digitButtons.indexOf(view)
            if (digit != -1) {
                shiftDigit(digit.toByte())
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        ui_digit_hour.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        ui_keypad_0.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        displayHeight = ui_digit_hour.measuredHeight
        keyPadHeight = ui_keypad_0.measuredHeight * 4
    }

}