package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 8/7/2016.
 */
class TimeRangePicker : FrameLayout, View.OnClickListener {

    enum class Granularity {
        DATE, TIME
    }

    private var suspendInvalidate: Boolean = false

    private var fromButton: Button
    private var toButton: Button
    private var durationIndicator: TextView

    val timeRangeChanged = Event<TimeSpan>()

    private var from: Long = TimeHelper.cutMillisecond(System.currentTimeMillis())
        set(value) {
            val processed: Long
            if (granularity == Granularity.DATE) {
                processed = TimeHelper.cutTimePartFromEpoch(value)
            } else {
                processed = TimeHelper.cutMillisecond(value)
            }

            if (field != processed) {
                field = processed

                suspendInvalidate = true
                to = Math.max(processed, to)
                suspendInvalidate = false
                invalidateViewSettings()
            }
        }


    private var to: Long = TimeHelper.cutMillisecond(System.currentTimeMillis())
        set(value) {
            val processed: Long
            if (granularity == Granularity.DATE) {
                processed = TimeHelper.cutTimePartFromEpoch(value)
            } else {
                processed = TimeHelper.cutMillisecond(value)
            }

            if (field != processed) {
                field = processed
                suspendInvalidate = true
                from = Math.min(processed, from)
                suspendInvalidate = false

                invalidateViewSettings()
            }
        }

    private lateinit var format: SimpleDateFormat

    fun getTimeSpan(): TimeSpan {
        return TimeSpan.fromPoints(from, to)
    }

    fun setTimeSpan(value: TimeSpan) {
        from = value.from
        to = value.from + value.duration
        invalidateViewSettings()
    }

    var granularity: Granularity by Delegates.observable(Granularity.TIME) {
        prop, old, new ->
        if (old != new) {
            invalidateDateFormat()
            invalidateViewSettings()
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_time_range_picker, this, false))


        fromButton = findViewById(R.id.ui_button_from) as Button
        toButton = findViewById(R.id.ui_button_to) as Button

        fromButton.setOnClickListener(this)
        toButton.setOnClickListener(this)

        durationIndicator = findViewById(R.id.ui_interval_indicator) as TextView

        InterfaceHelper.removeButtonTextDecoration(fromButton)
        InterfaceHelper.removeButtonTextDecoration(toButton)

        invalidateDateFormat()
        invalidateViewSettings()
    }

    private fun invalidateDateFormat() {
        val formatString = when (granularity) {
            Granularity.DATE ->
                context.resources.getString(R.string.dateformat_ymd)
            Granularity.TIME ->
                context.resources.getString(R.string.dateformat_minute)
        }

        format = SimpleDateFormat(formatString)

        if (granularity == Granularity.DATE) {
            from = TimeHelper.cutTimePartFromEpoch(from)
            to = TimeHelper.cutTimePartFromEpoch(to)
        } else if (granularity == Granularity.TIME) {
            from = TimeHelper.cutMillisecond(from)
            to = TimeHelper.cutMillisecond(to)
        }

    }

    private fun invalidateViewSettings() {
        if (!suspendInvalidate) {
            fromButton.text = format.format(Date(from))
            toButton.text = format.format(Date(to))
            durationIndicator.text = TimeHelper.durationToText(to - from, true, context)
        }
    }


    override fun onClick(button: View) {
        if (button is Button) {
            val activity = getActivity()
            if (activity != null) {

                val timestamp = if (button === fromButton) {
                    from
                } else if (button === toButton) {
                    to
                } else {
                    0
                }

                val cal = GregorianCalendar(TimeZone.getDefault())
                cal.timeInMillis = timestamp


                if (granularity == Granularity.DATE) {
                    //date picking


                    val dialog = DatePickerDialog.newInstance({
                        view, year, monthOfYear, dayOfMonth ->
                        cal.set(Calendar.YEAR, year)
                        cal.set(Calendar.MONTH, monthOfYear)
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)


                        val timestamp = cal.timeInMillis

                        val beforeFrom = from
                        val beforeTo = to

                        if (button === fromButton) {
                            from = timestamp
                        } else if (button === toButton) {
                            to = timestamp
                        }

                        if (beforeFrom != from || beforeTo != to) {
                            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
                        }
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                            .show(activity.fragmentManager, "DatePickerDialog")
                    /*
                    CalendarPickerDialogFragment.getInstance(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).showDialog(activity.supportFragmentManager) {
                        timestamp: Long, year: Int, month: Int, day: Int ->

                        val beforeFrom = from
                        val beforeTo = to

                        if (button === fromButton) {
                            from = timestamp
                        } else if (button === toButton) {
                            to = timestamp
                        }

                        if (beforeFrom != from || beforeTo != to) {
                            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
                        }
                    }*/
                } else {
                    //datetime picking
                    val datePickerDialog = DatePickerDialog.newInstance({ dialog, year, monthOfYear, dayOfMonth ->
                        cal.set(Calendar.YEAR, year)
                        cal.set(Calendar.MONTH, monthOfYear)
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                        val timePickerDialog = TimePickerDialog.newInstance({ dialog, hourOfDay, minute, second ->
                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val timestamp = cal.timeInMillis

                            val beforeFrom = from
                            val beforeTo = to

                            if (button === fromButton) {
                                from = timestamp
                            } else if (button === toButton) {
                                to = timestamp
                            }

                            if (beforeFrom != from || beforeTo != to) {
                                timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
                            }

                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false)
                        timePickerDialog.enableSeconds(false)
                        timePickerDialog.show(activity.fragmentManager, "TimePicker")

                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    datePickerDialog.setOkText(R.string.msg_next)
                    datePickerDialog.show(activity.fragmentManager, "DatePicker")

                    /*
                    DateTimePickerDialogFragment.getInstance(timestamp).showDialog(activity.supportFragmentManager) {
                        timestamp ->

                        val beforeFrom = from
                        val beforeTo = to

                        if (button === fromButton) {
                            from = timestamp
                        } else if (button === toButton) {
                            to = timestamp
                        }

                        if (beforeFrom != from || beforeTo != to) {
                            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
                        }
                    }*/
                }
            }
        }
    }
}