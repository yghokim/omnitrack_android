package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.component_timepoint.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.components.common.NumericUpDown
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.CalendarPickerDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.*
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates


/**
 * Created by younghokim on 16. 7. 22..
 */
class DateTimePicker : ConstraintLayout {
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
                ui_button_date.visibility = VISIBLE
        } else {
            ui_button_date.visibility = GONE
        }
    }


    //private lateinit var timeZoneSpinner: Spinner

    private val calendar = Calendar.getInstance()

    private lateinit var dateFormat: DateFormat

    private lateinit var hourNames: Array<String>

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


    private val pickerValueChangedHandler = { picker: Any, changeArgs: NumericUpDown.ChangeArgs ->

        val before = time

        when (mode) {
            SECOND -> {
                when (picker) {
                    ui_left_picker -> //hour_of_day
                    {
                        if (changeArgs.changeType == NumericUpDown.ChangeType.MANUAL)
                            calendar.set(Calendar.HOUR_OF_DAY, changeArgs.newValue)
                        else {
                            calendar.add(Calendar.HOUR_OF_DAY, changeArgs.delta)
                        }
                    }
                    ui_middle_picker -> //minute
                    {

                        if (changeArgs.changeType == NumericUpDown.ChangeType.MANUAL) {
                            calendar.set(Calendar.MINUTE, changeArgs.newValue)
                        } else {
                            calendar.add(Calendar.MINUTE, changeArgs.delta)
                        }
                    }
                    ui_right_picker -> //second
                    {
                        if (changeArgs.changeType == NumericUpDown.ChangeType.MANUAL) {
                            calendar.set(Calendar.SECOND, changeArgs.newValue)
                        } else {
                            calendar.add(Calendar.SECOND, changeArgs.delta)
                        }
                    }
                }
            }

            MINUTE -> {
                when (picker) {
                    ui_left_picker -> //hour
                    {
                        if (changeArgs.changeType == NumericUpDown.ChangeType.MANUAL) {
                            calendar.set(Calendar.HOUR_OF_DAY, (changeArgs.newValue % 12) + 12 * ui_right_picker.value)
                        } else {
                            calendar.add(Calendar.HOUR_OF_DAY, changeArgs.delta)
                        }
                    }
                    ui_middle_picker -> //minute
                    {
                        if (changeArgs.changeType == NumericUpDown.ChangeType.MANUAL) {
                            calendar.set(Calendar.MINUTE, changeArgs.newValue)
                        } else {
                            calendar.add(Calendar.MINUTE, changeArgs.delta)
                        }
                    }
                    ui_right_picker -> //ampm
                    {
                        calendar.set(Calendar.AM_PM, changeArgs.newValue)
                    }
                }
            }

            DATE -> {
                when (picker) {
                    ui_left_picker -> { //year
                        calendar.set(Calendar.YEAR, changeArgs.newValue)
                    }
                    ui_middle_picker -> { //month
                        calendar.set(Calendar.DAY_OF_MONTH, Math.min(calendar.get(Calendar.DAY_OF_MONTH), calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
                        calendar.set(Calendar.MONTH, changeArgs.newValue)
                    }
                    ui_right_picker -> { //day
                        calendar.set(Calendar.DAY_OF_MONTH, changeArgs.newValue)
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


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        inflateContent(R.layout.component_timepoint, true)

        ui_button_date.setOnClickListener {
            val activity = getActivity()
            if (activity != null) {
                CalendarPickerDialogFragment.getInstance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).showDialog(activity.supportFragmentManager) {
                    t, y, m, d ->
                    val before = calendar.timeInMillis
                    calendar.set(y, m, d)
                    if (before != calendar.timeInMillis) {
                        refresh()
                        timeChanged.invoke(this, time)
                    }
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


        ui_left_picker.zeroPad = 2
        ui_middle_picker.zeroPad = 2
        ui_right_picker.zeroPad = 2


        ui_left_picker.valueChanged += pickerValueChangedHandler
        ui_middle_picker.valueChanged += pickerValueChangedHandler
        ui_right_picker.valueChanged += pickerValueChangedHandler
    }


    fun setToPresent() {
        calendar.timeInMillis = System.currentTimeMillis()
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

        ui_left_picker.displayedValues = null
        ui_middle_picker.displayedValues = null
        ui_right_picker.displayedValues = null
        when (mode) {
            SECOND -> {
                //button shown, pickers are hour/minute/second
                if (isDayUsed)
                    ui_button_date.visibility = View.VISIBLE
                else
                    ui_button_date.visibility = View.GONE

                ui_button_date.text = dateFormat.format(calendar.time)

                ui_left_picker.minValue = 0
                ui_left_picker.maxValue = 23
                ui_left_picker.displayedValues = hourNames

                ui_left_picker.setValue(calendar.get(Calendar.HOUR_OF_DAY))

                ui_middle_picker.minValue = 0
                ui_middle_picker.maxValue = 59
                ui_middle_picker.setValue(calendar.get(Calendar.MINUTE))

                ui_right_picker.minValue = 0
                ui_right_picker.maxValue = 59
                ui_right_picker.setValue(calendar.get(Calendar.SECOND))
            }

            MINUTE -> {
                if (isDayUsed)
                    ui_button_date.visibility = View.VISIBLE
                else
                    ui_button_date.visibility = View.GONE

                ui_button_date.text = dateFormat.format(calendar.time)

                ui_left_picker.minValue = 1
                ui_left_picker.maxValue = 12

                ui_middle_picker.minValue = 0
                ui_middle_picker.maxValue = 59

                ui_right_picker.minValue = 0
                ui_right_picker.maxValue = 1
                ui_right_picker.displayedValues = resources.getStringArray(R.array.am_pm)


                ui_right_picker.setValue(calendar.getAmPm())

                ui_middle_picker.setValue(calendar.getMinute())

                val hour = calendar.getHour()
                ui_left_picker.setValue(if (hour == 0) {
                    12
                } else {
                    hour
                })

            }

            DATE -> {
                //button removed, pickers are year/month/day
                ui_button_date.visibility = View.GONE

                ui_left_picker.minValue = 1950
                ui_left_picker.maxValue = 2050
                ui_left_picker.setValue(calendar.get(Calendar.YEAR))

                ui_middle_picker.minValue = 0
                ui_middle_picker.maxValue = 11
                ui_middle_picker.setValue(calendar.get(Calendar.MONTH))
                ui_middle_picker.displayedValues = monthNames

                ui_right_picker.minValue = 1
                ui_right_picker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                ui_right_picker.setValue(calendar.get(Calendar.DAY_OF_MONTH))
            }
        }
    }

}