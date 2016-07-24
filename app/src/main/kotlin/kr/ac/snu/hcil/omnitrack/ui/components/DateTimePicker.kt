package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 22..
 */
class DateTimePicker(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), NumberPicker.OnValueChangeListener {

    companion object {
        const val DATE = 0
        const val TIME = 1

        val availableTimeZones = TimeZone.getAvailableIDs().map {
            TimeZone.getTimeZone(it)
        }

        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    private lateinit var leftPicker: NumberPicker
    private lateinit var middlePicker: NumberPicker
    private lateinit var rightPicker: NumberPicker

    private lateinit var dateButton: Button

    //private lateinit var timeZoneSpinner: Spinner

    private var calendar = Calendar.getInstance()

    private lateinit var dateFormat: SimpleDateFormat

    var mode: Int by Delegates.observable(-1)
    {
        prop, old, new ->
        if (old != new) {
            refresh()
        }
    }

    var time: TimePoint
        get() {
            return TimePoint(calendar.timeInMillis, calendar.timeZone.id)
        }
        set(value) {
            calendar.timeInMillis = value.timestamp
            refresh()
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_timepoint, this, false))

        leftPicker = findViewById(R.id.leftPicker) as NumberPicker
        middlePicker = findViewById(R.id.middlePicker) as NumberPicker
        rightPicker = findViewById(R.id.rightPicker) as NumberPicker

        leftPicker.setOnValueChangedListener(this)
        middlePicker.setOnValueChangedListener(this)
        rightPicker.setOnValueChangedListener(this)



        dateButton = findViewById(R.id.ui_button_date) as Button

        dateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_ymd))

        /*
        timeZoneSpinner = findViewById(R.id.ui_time_zone_spinner) as Spinner

        val timeZoneAdapter = ArrayAdapter<TimeZone>(context, android.R.layout.simple_spinner_dropdown_item, availableTimeZones)

        timeZoneSpinner.adapter = timeZoneAdapter
        timeZoneSpinner.setSelection(availableTimeZones.indices.maxBy { availableTimeZones[it].id == TimeZone.getDefault().id }!!)*/

        mode = TIME
    }

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        if (oldVal != newVal) {
            when (mode) {
                TIME -> {
                    when (picker) {
                        leftPicker -> //hour
                        {
                            calendar.set(Calendar.HOUR_OF_DAY, newVal)
                        }
                        middlePicker -> //minute
                        {
                            calendar.set(Calendar.MINUTE, newVal)
                        }
                        rightPicker -> //second
                        {
                            calendar.set(Calendar.SECOND, newVal)
                        }
                    }
                }

                DATE -> {
                    when (picker) {
                        leftPicker -> { //year
                            calendar.set(Calendar.YEAR, newVal)
                        }
                        middlePicker -> { //month
                            calendar.set(Calendar.DAY_OF_MONTH, Math.min(calendar.get(Calendar.DAY_OF_MONTH), calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
                            calendar.set(Calendar.MONTH, newVal + 1)
                        }
                        rightPicker -> { //day
                            calendar.set(Calendar.DAY_OF_MONTH, newVal)
                        }
                    }
                }
            }

            //println(calendar.toString())
        }
    }


    fun setToCurrent() {
        calendar = Calendar.getInstance()
        refresh()
    }

    private fun refresh() {
        when (mode) {
            TIME -> {
                //button shown, pickers are hour/minute/second
                dateButton.visibility = View.VISIBLE
                dateButton.text = dateFormat.format(calendar.time)

                leftPicker.minValue = 0
                leftPicker.maxValue = 23
                leftPicker.value = calendar.get(Calendar.HOUR_OF_DAY)

                middlePicker.minValue = 0
                middlePicker.maxValue = 59
                middlePicker.displayedValues = null
                middlePicker.value = calendar.get(Calendar.MINUTE)

                rightPicker.minValue = 0
                rightPicker.maxValue = 59
                rightPicker.value = calendar.get(Calendar.SECOND)
            }

            DATE -> {
                //button removed, pickers are year/month/day
                dateButton.visibility = View.GONE

                leftPicker.minValue = 1950
                leftPicker.maxValue = 2050
                leftPicker.value = calendar.get(Calendar.YEAR)

                middlePicker.minValue = 0
                middlePicker.maxValue = 11
                middlePicker.value = calendar.get(Calendar.MONTH) - 1
                middlePicker.displayedValues = monthNames

                rightPicker.minValue = 1
                rightPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                rightPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
            }
        }
    }

}