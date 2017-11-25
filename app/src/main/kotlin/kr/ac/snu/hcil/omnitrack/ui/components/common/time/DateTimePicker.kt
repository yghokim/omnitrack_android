package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.components.common.NumericUpDown
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.CalendarPickerDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.getAmPm
import kr.ac.snu.hcil.omnitrack.utils.getHour
import kr.ac.snu.hcil.omnitrack.utils.getMinute
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 22..
 */
class DateTimePicker(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    companion object {
        const val DATE = 0
        const val MINUTE = 1
        const val SECOND = 2
        /*
                val availableTimeZones = TimeZone.getAvailableIDs().map {
                    TimeZone.getTimeZone(it)
                }
        */
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    var isDayUsed: Boolean by Delegates.observable(true)
    {
        prop, old, new ->
        if (new) {
            if (mode != DATE)
                dateButton.visibility = VISIBLE
        } else {
            dateButton.visibility = GONE
        }
    }

    private lateinit var leftPicker: NumericUpDown
    private lateinit var middlePicker: NumericUpDown
    private lateinit var rightPicker: NumericUpDown

    private var dateButton: Button

    //private lateinit var timeZoneSpinner: Spinner

    private var calendar = Calendar.getInstance()

    private var dateFormat: DateFormat

    private var hourNames: Array<String>

    var mode: Int by Delegates.observable(-1)
    {
        prop, old, new ->
        if (old != new) {
            refresh()
        }
    }

    var time: TimePoint = TimePoint()
        get() {
            return TimePoint(calendar.timeInMillis, calendar.timeZone.id)
        }
        set(value) {
            if (field != value) {
                field = value
                calendar.timeInMillis = value.timestamp
                refresh()
                timeChanged.invoke(this, value)
            }
        }

    val hour: Int
        get() = calendar.getHour()

    val minute: Int
        get() = calendar.getMinute()

    val amPm: Int
        get() = calendar.getAmPm()


    private val pickerValueChangedHandler = {
        picker: Any, newVal: Int ->

        val before = time

        when (mode) {
            SECOND -> {
                when (picker) {
                    leftPicker -> //hour_of_day
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

            MINUTE -> {
                when (picker) {
                    leftPicker -> //hour
                    {
                        calendar.set(Calendar.HOUR_OF_DAY, (newVal % 12) + 12 * rightPicker.value)
                    }
                    middlePicker -> //minute
                    {
                        calendar.set(Calendar.MINUTE, newVal)
                    }
                    rightPicker -> //ampm
                    {
                        calendar.set(Calendar.AM_PM, newVal)
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
                        calendar.set(Calendar.MONTH, newVal)
                    }
                    rightPicker -> { //day
                        calendar.set(Calendar.DAY_OF_MONTH, newVal)
                    }
                }
            }
        }

        refresh()
        val current = time
        if (before != current) {
            timeChanged.invoke(this, current)
        }
    }

    val timeChanged = Event<TimePoint>()

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_timepoint, this, false))

        leftPicker = findViewById(R.id.ui_left_picker)
        middlePicker = findViewById(R.id.ui_middle_picker)
        rightPicker = findViewById(R.id.ui_right_picker)

        dateButton = findViewById(R.id.ui_button_date)

        dateButton.setOnClickListener {
            val activity = getActivity()
            if (activity != null) {
                CalendarPickerDialogFragment.getInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).showDialog(activity.supportFragmentManager) {
                    t, y, m, d ->
                    calendar.set(y, m, d)
                    refresh()
                }
            }
        }

        dateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_ymd))
        //android.text.format.DateFormat.getMediumDateFormat()

        hourNames = Array<String>(24) {
            index ->
            String.format(resources.getString(if (index < 12) {
                R.string.format_hour_am
            } else {
                R.string.format_hour_pm
            }), String.format("%02d", if (index == 12) {
                12
            } else {
                index % 12
            }))
        }

        /*
        timeZoneSpinner = findViewById(R.id.ui_time_zone_spinner) as Spinner

        val timeZoneAdapter = ArrayAdapter<TimeZone>(context, android.R.layout.simple_spinner_dropdown_item, availableTimeZones)

        timeZoneSpinner.adapter = timeZoneAdapter
        timeZoneSpinner.setSelection(availableTimeZones.indices.maxBy { availableTimeZones[it].id == TimeZone.getDefault().id }!!)*/

        mode = MINUTE


        leftPicker.zeroPad = 2
        middlePicker.zeroPad = 2
        rightPicker.zeroPad = 2


        leftPicker.valueChanged += pickerValueChangedHandler
        middlePicker.valueChanged += pickerValueChangedHandler
        rightPicker.valueChanged += pickerValueChangedHandler
    }


    fun setToPresent() {
        calendar = Calendar.getInstance()
        refresh()
        timeChanged.invoke(this, time)
    }

    fun setTime(hour: Int, minute: Int, amPm: Int) {
        if (this.hour != hour || this.minute != minute || this.amPm != amPm) {
            calendar.set(Calendar.HOUR, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.AM_PM, amPm)
            refresh()
            timeChanged.invoke(this, time)
        }
    }

    private fun refresh() {

        leftPicker.displayedValues = null
        middlePicker.displayedValues = null
        rightPicker.displayedValues = null

        when (mode) {
            SECOND -> {
                //button shown, pickers are hour/minute/second
                if (isDayUsed)
                    dateButton.visibility = View.VISIBLE
                else
                    dateButton.visibility = View.GONE

                dateButton.text = dateFormat.format(calendar.time)

                leftPicker.minValue = 0
                leftPicker.maxValue = 23
                leftPicker.displayedValues = hourNames

                leftPicker.value = calendar.get(Calendar.HOUR_OF_DAY)

                middlePicker.minValue = 0
                middlePicker.maxValue = 59
                middlePicker.value = calendar.get(Calendar.MINUTE)

                rightPicker.minValue = 0
                rightPicker.maxValue = 59
                rightPicker.value = calendar.get(Calendar.SECOND)
            }

            MINUTE -> {
                if (isDayUsed)
                    dateButton.visibility = View.VISIBLE
                else
                    dateButton.visibility = View.GONE

                dateButton.text = dateFormat.format(calendar.time)

                leftPicker.minValue = 1
                leftPicker.maxValue = 12

                middlePicker.minValue = 0
                middlePicker.maxValue = 59

                rightPicker.minValue = 0
                rightPicker.maxValue = 1
                rightPicker.displayedValues = arrayOf("AM", "PM")


                rightPicker.value = calendar.getAmPm()

                middlePicker.value = calendar.getMinute()

                val hour = calendar.getHour()
                leftPicker.value = if (hour == 0) {
                    12
                } else {
                    hour
                }

            }

            DATE -> {
                //button removed, pickers are year/month/day
                dateButton.visibility = View.GONE

                leftPicker.minValue = 1950
                leftPicker.maxValue = 2050
                leftPicker.value = calendar.get(Calendar.YEAR)

                middlePicker.minValue = 0
                middlePicker.maxValue = 11
                middlePicker.value = calendar.get(Calendar.MONTH)
                middlePicker.displayedValues = monthNames

                rightPicker.minValue = 1
                rightPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                rightPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
            }
        }
    }

}