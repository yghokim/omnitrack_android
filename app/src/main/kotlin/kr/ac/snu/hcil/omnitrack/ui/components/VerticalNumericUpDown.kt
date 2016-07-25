package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Display
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.text.ParseException
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-25.
 */
class VerticalNumericUpDown(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var minValue: Int = 0
        set(value) {
            field = Math.min(maxValue - 1, value)
            this.value = this.value
        }

    var maxValue: Int = 10
        set(value) {
            field = Math.max(minValue + 1, value)
            this.value = this.value
        }

    var value: Int = 0
        set(value) {
            val clamped = Math.max(minValue, Math.min(maxValue, value))
            if (field != clamped) {
                field = clamped
                invalidateViews()
                valueChanged.invoke(this, clamped)
            }
        }

    var displayedValues: Array<String>? = null
        set(value) {
            field = value
            invalidateViews()
        }

    val valueChanged = Event<Int>()

    private lateinit var upButton: ImageButton
    private lateinit var downButton: ImageButton

    private lateinit var field: TextView
    init {
        orientation = LinearLayout.VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_number_picker_vertical, this, true)
        upButton = findViewById(R.id.ui_button_plus) as ImageButton
        upButton.setOnClickListener {
            view ->
            value = if (value + 1 > maxValue) {
                minValue
            } else {
                value + 1
            }
        }

        downButton = findViewById(R.id.ui_button_minus) as ImageButton
        downButton.setOnClickListener {
            view ->
            value = if (value - 1 < minValue) {
                maxValue
            } else {
                value - 1
            }
        }

        field = findViewById(R.id.valueField) as TextView

        invalidateViews()
    }

    private fun getDisplayedValue(value: Int): String {
        return displayedValues?.get(value - minValue) ?: value.toString()
    }

    private fun invalidateViews() {
        field.setText(getDisplayedValue(value), TextView.BufferType.NORMAL)
    }
}