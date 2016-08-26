package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/25/2016.
 */
class HourPicker : RelativeLayout, View.OnClickListener {


    companion object {
        val oClockNames = Array<String>(12) {
            index ->
            String.format("%02d", if (index == 0) 12 else index) + ":00"
        }
    }

    /**
     * am:0, pm:1
     */
    var amPm: Int = 0
        set(value) {
            if (field != value) {
                field = value
                if (value == 0)
                    amPmView.setText(R.string.time_am)
                else
                    amPmView.setText(R.string.time_pm)
            }
        }

    /**
     * 0~11
     */
    var hour: Int = 0
        set(value) {
            if (field != value % 12) {
                field = value % 12
                hourView.text = oClockNames[field]
            }
        }

    var hourOfDay: Int
        get() {
            return hour + 12 * amPm
        }
        set(value) {
            if (hourOfDay != value) {
                hour = value % 12
                amPm = if (value >= 12) 1 else 0
            }
        }


    private val amPmView: TextView
    private val hourView: TextView

    private val upButton: View
    private val downButton: View

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_hour_picker, this, true)

        amPmView = findViewById(R.id.ui_ampm) as TextView
        hourView = findViewById(R.id.ui_hour) as TextView

        amPm = 1
        hour = 9

        upButton = findViewById(R.id.ui_button_up)
        downButton = findViewById(R.id.ui_button_down)

        amPmView.setOnClickListener(this)
        upButton.setOnClickListener(this)
        downButton.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (view === upButton) {
            hour = (hour + 1) % 12
            if (hour == 0) {
                amPm = (1 - amPm)
            }
        } else if (view === downButton) {
            hour = if (hour == 0) 11 else (hour - 1)
            if (hour == 11) {
                amPm = (1 - amPm)
            }
        } else if (view === amPmView) {
            amPm = 1 - amPm
        }
    }
}