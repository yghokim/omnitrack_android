package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.*
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

    companion object {
        const val MODE_PLUS_MINUS = 0
        const val MODE_UP_DOWN = 1

        const val FAST_CHANGE_INTERVAL = 100L
    }

    var minValue: Int = Int.MIN_VALUE
        set(value) {
            field = Math.min(maxValue - 1, value)
            this.value = this.value
        }

    var maxValue: Int = Int.MAX_VALUE
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

    val valueChanged = Event<Int>()

    private lateinit var upButton: ImageButton
    private lateinit var downButton: ImageButton

    private lateinit var field: TextView

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

        invalidateViews()
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
        value = if (value + 1 > maxValue) {
            minValue
        } else {
            value + 1
        }
    }

    fun decrease() {
        value = if (value - 1 < minValue) {
            maxValue
        } else {
            value - 1
        }
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