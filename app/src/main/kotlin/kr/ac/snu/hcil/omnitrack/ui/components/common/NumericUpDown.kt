package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class NumericUpDown : LinearLayout, OnLongClickListener, OnClickListener, OnTouchListener {

    enum class ChangeType { INC, DEC, MANUAL }
    data class ChangeArgs(val newValue: Int, val changeType: ChangeType, val delta: Int)

    companion object {

        const val MODE_PLUS_MINUS = 0
        const val MODE_UP_DOWN = 1

        const val FAST_CHANGE_INTERVAL = 100L
    }

    var minValue: Int = Int.MIN_VALUE
        set(value) {
            field = Math.min(maxValue - 1, value)
        }

    var maxValue: Int = Int.MAX_VALUE
        set(value) {
            field = Math.max(minValue + 1, value)
        }

    var value: Int = 0
        private set

    fun setValue(newValue: Int, changeType: ChangeType = ChangeType.MANUAL, delta: Int = 0) {
        val clamped = Math.max(minValue, Math.min(maxValue, newValue))
        if (value != clamped) {
            value = clamped
            valueChanged.invoke(this, ChangeArgs(clamped, changeType, delta))
        }
        invalidateViews()
    }

    var displayedValues: Array<String>? = null
        set(value) {
            field = value
            invalidateViews()
            this.field.isEnabled = value == null
            this.field.isFocusableInTouchMode = value == null
        }

    var quantityResId: Int? = null
        set(value) {
            if (field != value) {
                field = value
                if (quantityResId != null) {
                    displayedValues = null
                } else invalidateViews()
            }
        }

    var zeroPad: Int by Delegates.observable(0) {
        prop, old, new ->
        if (old != new) {
            invalidateViews()
        }
    }

    val valueChanged = Event<ChangeArgs>()

    private lateinit var upButton: ImageButton
    private lateinit var downButton: ImageButton

    private lateinit var field: EditText

    var allowLongPress: Boolean = true

    private var timer: Timer? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        changeDirection(orientation)
    }

    constructor(context: Context?) : super(context) {
        changeDirection(LinearLayout.VERTICAL)
    }


    private fun changeDirection(direction: Int) {
        orientation = direction

        when (direction) {
            LinearLayout.VERTICAL -> {
                inflateContent(R.layout.component_number_picker_vertical, true)
            }
            LinearLayout.HORIZONTAL -> {
                inflateContent(R.layout.component_number_picker_horizontal, true)
            }

        }

        upButton = findViewById(R.id.ui_button_plus)
        upButton.setOnClickListener(this)
        upButton.setOnLongClickListener(this)
        upButton.setOnTouchListener(this)

        downButton = findViewById(R.id.ui_button_minus)
        downButton.setOnClickListener(this)
        downButton.setOnLongClickListener(this)
        downButton.setOnTouchListener(this)

        field = findViewById(R.id.ui_value_field)
        field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                s.toString().toIntOrNull()?.let {
                    value ->

                    if (value == this@NumericUpDown.value) {
                        return
                    }

                    val maxValueStringLength = maxValue.toString().length
                    if (s.toString().length >= maxValueStringLength) {
                        setValue(value, ChangeType.MANUAL)
                        field.setSelection(0, field.text.length)
                    }
                }
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        })

        field.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                field.post {
                    field.setSelection(0, field.text.length)
                }
            } else {
                field.text.toString().toIntOrNull()?.let {
                    value -> setValue(value, ChangeType.MANUAL)
                }
            }
        }

        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                field.text.toString().toIntOrNull()?.let {
                    value -> setValue(value, ChangeType.MANUAL)
                }
                clearEditFocus()

//                val nextView = if (direction == LinearLayout.HORIZONTAL) {
//                    focusSearch(View.FOCUS_DOWN)
//                } else {
//                    focusSearch(View.FOCUS_RIGHT)
//                }
//
//                if (nextView == null || !nextView.requestFocus(View.FOCUS_FORWARD)) {
//                    clearEditFocus()
//                }
                true
            } else {
                false
            }
        }
        invalidateViews()
    }

    private fun clearEditFocus() {
        clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onClick(view: View) {
        if (view === downButton) {
            decrease()
        } else if (view === upButton) {
            increase()
        }
    }

    override fun onLongClick(view: View): Boolean {
        if (allowLongPress) {
            if (view === upButton) {
                startFastChange(true)
            } else if (view === downButton) {
                startFastChange(false)
            }
        }

        return false
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (view === upButton || view === downButton) {
                stopFastChange()
            }
        }

        return false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopFastChange()
    }

    fun startFastChange(increase: Boolean) {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (increase) {
                        increase()
                    } else {
                        decrease()
                    }
                }
            }

        }, 0, FAST_CHANGE_INTERVAL)
    }

    fun stopFastChange() {
        timer?.cancel()
        timer = null
    }

    fun increase() {
        setValue(if (value + 1 > maxValue) {
            minValue
        } else {
            value + 1
        }, ChangeType.INC, 1)
    }

    fun decrease() {
        setValue(if (value - 1 < minValue) {
            maxValue
        } else {
            value - 1
        }, ChangeType.DEC, -1)
    }

    private fun getDisplayedValue(value: Int): String {
        return displayedValues?.get(value - minValue) ?: if (zeroPad > 1) {
            String.format("%0${zeroPad}d", value)
        } else if (quantityResId != null) {
            context.resources.getQuantityString(quantityResId!!, value)
        } else value.toString()
    }

    private fun invalidateViews() {
        field.setText(getDisplayedValue(value), TextView.BufferType.NORMAL)
    }

}