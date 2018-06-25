package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.os.Parcel
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.INumericUpDown.Companion.FAST_CHANGE_INTERVAL
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import uk.co.chrisjenx.calligraphy.CalligraphyUtils
import java.util.*
import kotlin.properties.Delegates

class NumericUpDownImpl(val context: Context, attrs: AttributeSet?, val view: View) : View.OnLongClickListener, View.OnClickListener, View.OnTouchListener, INumericUpDown {

    override var minValue: Int = Int.MIN_VALUE
        set(value) {
            field = Math.min(maxValue - 1, value)
            _value = getClampValue(_value)
        }

    override var maxValue: Int = Int.MAX_VALUE
        set(value) {
            field = Math.max(minValue + 1, value)
            _value = getClampValue(_value)
        }

    override val value: Int get() = _value

    private var _value: Int = 0

    private fun getClampValue(value: Int): Int {
        return Math.max(minValue, Math.min(maxValue, value))
    }

    override fun setValue(newValue: Int, changeType: INumericUpDown.ChangeType, delta: Int) {
        val clamped = getClampValue(newValue)
        if (_value != clamped) {
            _value = clamped
            valueChanged.invoke(view, INumericUpDown.ChangeArgs(_value, changeType, delta))
        }
        invalidateViews()
    }

    override var displayedValues: Array<String>? = null
        set(value) {
            field = value
            if (value != null) {
                suspendInvalidateValue = true
                formatter = null
                suspendInvalidateValue = false
            }
            this.field.isFocusableInTouchMode = value == null
            this.field.isEnabled = value == null
            invalidateViews()
        }

    override var formatter: ((Int) -> String)? = null
        set(value) {
            field = value
            if (value != null) {
                suspendInvalidateValue = true
                displayedValues = null
                suspendInvalidateValue = false
            }
            invalidateViews()
        }

    override var quantityResId: Int? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    suspendInvalidateValue = true
                    displayedValues = null
                    suspendInvalidateValue = false
                }
                invalidateViews()
            }
        }

    override var zeroPad: Int by Delegates.observable(0) { prop, old, new ->
        if (old != new) {
            invalidateViews()
        }
    }

    override val valueChanged = Event<INumericUpDown.ChangeArgs>()

    private val upButton: ImageButton
    private val downButton: ImageButton

    private val field: EditText

    override var allowLongPress: Boolean = true

    private var timer: Timer? = null

    private var suspendInvalidateValue: Boolean = false

    init {

        upButton = view.findViewById(R.id.ui_button_plus)
        upButton.setOnClickListener(this)
        upButton.setOnLongClickListener(this)
        upButton.setOnTouchListener(this)

        downButton = view.findViewById(R.id.ui_button_minus)
        downButton.setOnClickListener(this)
        downButton.setOnLongClickListener(this)
        downButton.setOnTouchListener(this)

        field = view.findViewById(R.id.ui_value_field)
        field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                s.toString().toIntOrNull()?.let {
                    value ->

                    if (value == _value) {
                        return
                    }

                    val maxValueStringLength = maxValue.toString().length
                    if (s.toString().length >= maxValueStringLength) {
                        setValue(value, INumericUpDown.ChangeType.MANUAL)
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
                val currValue = field.text.toString().toIntOrNull()
                if (currValue == null) {
                    setValue(minValue, INumericUpDown.ChangeType.MANUAL)
                } else {
                    setValue(currValue, INumericUpDown.ChangeType.MANUAL)
                }
            }
        }

        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val currValue = field.text.toString().toIntOrNull()
                if (currValue == null) {
                    setValue(minValue, INumericUpDown.ChangeType.MANUAL)
                } else {
                    setValue(currValue, INumericUpDown.ChangeType.MANUAL)
                }
                clearEditFocus()
                true
            } else {
                false
            }
        }
        invalidateViews()

        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(android.R.attr.textSize),
                0, 0)

        try {
            if (a.hasValue(0))
                field.textSize = a.getDimension(0, 12f)
        } finally {
            a.recycle()
        }

        val b = context.obtainStyledAttributes(attrs, R.styleable.NumericUpDown)
        try {

            if (b.hasValue(R.styleable.NumericUpDown_digitFontPath)) {
                CalligraphyUtils.applyFontToTextView(context, field, b.getString(R.styleable.NumericUpDown_digitFontPath))
            }
        } finally {
            b.recycle()
        }
    }

    private fun clearEditFocus() {
        view.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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

    fun onDetachedFromWindow() {
        stopFastChange()
    }

    fun startFastChange(increase: Boolean) {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                view.handler.post {
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
        }, INumericUpDown.ChangeType.INC, 1)
    }

    fun decrease() {
        setValue(if (value - 1 < minValue) {
            maxValue
        } else {
            value - 1
        }, INumericUpDown.ChangeType.DEC, -1)
    }

    private fun getDisplayedValue(value: Int): String {
        return formatter?.invoke(value) ?: displayedValues?.get(value - minValue)
        ?: if (zeroPad > 1) {
            String.format("%0${zeroPad}d", value)
        } else if (quantityResId != null) {
            context.resources.getQuantityString(quantityResId!!, value)
        } else value.toString()
    }

    private fun invalidateViews() {
        if (!suspendInvalidateValue) {
            field.setText(getDisplayedValue(value), TextView.BufferType.NORMAL)
        }
    }

    data class StateData(var minValue: Int = Int.MIN_VALUE,
                         var maxValue: Int = Int.MAX_VALUE,
                         var value: Int = 0,
                         var displayedValues: Array<String>? = null,
                         var quantityResId: Int? = null,
                         var zeroPad: Int = 2,
                         var allowLongPress: Boolean = false) {
        fun readFromParcel(source: Parcel) {
            minValue = source.readInt()
            maxValue = source.readInt()
            value = source.readInt()
            displayedValues = source.createStringArray()
            quantityResId = source.readSerializable() as Int
            zeroPad = source.readInt()
            allowLongPress = source.readByte() == 0.toByte()
        }

        fun writeToParcel(out: Parcel) {
            out.writeInt(minValue)
            out.writeInt(maxValue)
            out.writeInt(value)
            out.writeStringArray(displayedValues)
            out.writeSerializable(quantityResId)
            out.writeInt(zeroPad)
            out.writeByte(if (allowLongPress) 1.toByte() else 0.toByte())
        }
    }

    fun makeStateData(): StateData {
        return StateData(minValue, maxValue, value, displayedValues, quantityResId, zeroPad, allowLongPress)
    }

    fun applyStateData(state: StateData) {
        suspendInvalidateValue = true
        valueChanged.suspend = true

        minValue = state.minValue
        maxValue = state.maxValue
        _value = state.value
        displayedValues = state.displayedValues
        quantityResId = state.quantityResId
        zeroPad = state.zeroPad
        allowLongPress = allowLongPress

        valueChanged.suspend = false
        suspendInvalidateValue = false
    }
}