package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper

/**
 * Created by younghokim on 16. 8. 23..
 */
class DurationPicker : FrameLayout, View.OnClickListener {

    companion object {
        const val MAX_DURATION_SECONDS = 99 * 3600 + 99 * 60 + 99
    }

    private val display: View
    private val hourView: TextView
    private val minuteView: TextView
    private val secondView: TextView
    private val backspaceButton: View

    private val keyPad: View

    private val digitButtons: Array<View>
    private val digitButton00: View
    private val digitButtonUp: View


    private val digits = Array<Byte>(6) { 0 }

    private var suspendRefreshDigits = false


    private var mIsInInputMode: Boolean = false

    val isInInputMode: Boolean get() = mIsInInputMode

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
                    different = different % TimeHelper.hoursInMilli.toInt()

                    var elapsedMinutes = different / TimeHelper.minutesInMilli.toInt()
                    different = different % TimeHelper.minutesInMilli.toInt()

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
            }

        }


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.component_duration_picker, this)

        display = findViewById(R.id.ui_duration_picker_display)

        hourView = findViewById(R.id.ui_digit_hour) as TextView
        minuteView = findViewById(R.id.ui_digit_minute) as TextView
        secondView = findViewById(R.id.ui_digit_second) as TextView

        keyPad = findViewById(R.id.ui_keypad)

        digitButtons = arrayOf(
                findViewById(R.id.ui_keypad_0),
                findViewById(R.id.ui_keypad_1),
                findViewById(R.id.ui_keypad_2),
                findViewById(R.id.ui_keypad_3),
                findViewById(R.id.ui_keypad_4),
                findViewById(R.id.ui_keypad_5),
                findViewById(R.id.ui_keypad_6),
                findViewById(R.id.ui_keypad_7),
                findViewById(R.id.ui_keypad_8),
                findViewById(R.id.ui_keypad_9)
        )

        digitButton00 = findViewById(R.id.ui_keypad_00)
        digitButtonUp = findViewById(R.id.ui_keypad_up)

        display.setOnClickListener(this)

        backspaceButton = findViewById(R.id.ui_button_backspace)
        backspaceButton.setOnClickListener(this)

        for (button in digitButtons)
            button.setOnClickListener(this)

        digitButton00.setOnClickListener(this)
        digitButtonUp.setOnClickListener(this)
    }

    fun setInputMode(mode: Boolean, animate: Boolean) {
        if (mIsInInputMode != mode) {
            mIsInInputMode = mode

            if (animate) {
                TransitionManager.beginDelayedTransition(this)
            }

            if (mode) {
                backspaceButton.visibility = View.VISIBLE
                keyPad.visibility = View.VISIBLE
            } else {
                backspaceButton.visibility = View.GONE
                keyPad.visibility = View.GONE
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

}