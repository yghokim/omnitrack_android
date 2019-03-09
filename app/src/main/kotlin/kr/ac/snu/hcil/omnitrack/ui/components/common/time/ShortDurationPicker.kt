package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.component_property_duration_picker.view.*
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R

class ShortDurationPicker : ConstraintLayout {

    companion object {
        const val UNIT_HOUR = 0
        const val UNIT_MINUTE = 1
        const val UNIT_SECOND = 2

        fun getTimeMultiplier(unit: Int): Int = when (unit) {
            UNIT_HOUR -> 3600
            UNIT_MINUTE -> 60
            UNIT_SECOND -> 1
            else -> 1
        }
    }

    var durationSeconds: Int
        get() = ui_digit_input.text.toString().toInt() * getTimeMultiplier(ui_unit_spinner.selectedIndex)
        set(value) {
            if (durationSeconds != value) {
                println("duration seconds : $value")
                var idealUnit: Int = UNIT_SECOND
                for (unit in 0..2) {
                    val multiplier = getTimeMultiplier(unit)
                    if (value / multiplier > 0 && (value / multiplier) * multiplier == value) {
                        idealUnit = unit
                        break
                    }
                }
                ui_unit_spinner.selectedIndex = idealUnit
                ui_digit_input.setText((value / getTimeMultiplier(idealUnit)).toString(), TextView.BufferType.NORMAL)

                durationChanged.invoke(this, value)
            }
        }

    val durationChanged = Event<Int>()

    var max: Int = Int.MAX_VALUE

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context, attrs)
    }

    init {
        inflateContent(R.layout.component_property_duration_picker, true)

        ui_unit_spinner.setItems(resources.getString(R.string.time_duration_hour_full), resources.getString(R.string.time_duration_minute_full), resources.getString(R.string.time_duration_second_full))

        ui_unit_spinner.selectedIndex = UNIT_MINUTE

        ui_digit_input.setText("5", TextView.BufferType.NORMAL)

        ui_unit_spinner.setOnItemSelectedListener { view, position, id, item ->
            durationChanged.invoke(this, durationSeconds)
        }

        ui_digit_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.toString() == "0" || (s.toString().toIntOrNull() ?: -1 < 0) || s.toString().isBlank()) {
                    ui_digit_input.setText("1", TextView.BufferType.NORMAL)
                } else if (s.toString().toInt() * getTimeMultiplier(ui_unit_spinner.selectedIndex) > max) {
                    ui_digit_input.setText((max / getTimeMultiplier(ui_unit_spinner.selectedIndex)).toString(), TextView.BufferType.NORMAL)
                } else {
                    durationChanged.invoke(this, durationSeconds)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(
                attrs, intArrayOf(android.R.attr.text),
                0, 0)

        try {
            if (a.hasValue(0))
                ui_title.text = a.getString(0)
        } finally {
            a.recycle()
        }

        val b = context.obtainStyledAttributes(attrs, R.styleable.ShortDurationPicker)
        try {
            if (b.hasValue(R.styleable.ShortDurationPicker_rawDurationSeconds)) {
                durationSeconds = b.getInteger(R.styleable.ShortDurationPicker_rawDurationSeconds, 0)
            } else {
                if (b.hasValue(R.styleable.ShortDurationPicker_durationUnit)) {
                    when (b.getString(R.styleable.ShortDurationPicker_durationUnit).toLowerCase()) {
                        "hour", "hours", "hr", "hrs" -> {
                            ui_unit_spinner.selectedIndex = UNIT_HOUR
                        }
                        "minute", "minutes", "min", "mins" -> {
                            ui_unit_spinner.selectedIndex = UNIT_MINUTE
                        }
                        "second", "seconds", "sec", "secs" -> {
                            ui_unit_spinner.selectedIndex = UNIT_SECOND
                        }
                    }
                }

                if (b.hasValue(R.styleable.ShortDurationPicker_durationValue)) {
                    ui_digit_input.setText(b.getString(R.styleable.ShortDurationPicker_durationValue), TextView.BufferType.NORMAL)
                }
            }
        } finally {
            b.recycle()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (enabled) {
            ui_digit_input.isEnabled = true
            ui_unit_spinner.isEnabled = true
            this.alpha = 1.0f
        } else {
            ui_digit_input.isEnabled = false
            ui_unit_spinner.isEnabled = false
            this.alpha = 0.3f
        }
    }
}