package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.android.synthetic.main.component_time_range_picker.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
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
                //to = Math.max(processed, to)
                to = processed
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


    private val dateTimeDialogPicker: ImmersiveDateTimePicker by lazy {
        ImmersiveDateTimePicker(context)
    }
    private val datetimeDialog: Dialog by lazy {
        val builder = MaterialDialog.Builder(context)
        builder.customView(dateTimeDialogPicker, false)
        builder.autoDismiss(true)
        val dialog = builder.build()
        dateTimeDialogPicker.setOnApplyButtonClickedListener(OnClickListener {
            val beforeFrom = from
            val beforeTo = to

            if (dateTimeDialogPicker.tag == "from") {
                from = dateTimeDialogPicker.value
            } else if (dateTimeDialogPicker.tag == "to") {
                to = dateTimeDialogPicker.value
            }

            if (beforeFrom != from || beforeTo != to) {
                timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
            }
            dialog.dismiss()
        })

        return@lazy dialog
    }

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


        fromButton = findViewById(R.id.ui_button_from)
        toButton = findViewById(R.id.ui_button_to)

        fromButton.setOnClickListener(this)
        toButton.setOnClickListener(this)

        ui_button_preset_1.setOnClickListener(this)
        ui_button_preset_2.setOnClickListener(this)
        ui_button_preset_now.setOnClickListener(this)

        ui_button_up.setOnClickListener(this)
        ui_button_down.setOnClickListener(this)

        durationIndicator = findViewById(R.id.ui_interval_indicator)

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

            if (granularity == Granularity.DATE) {
                ui_button_preset_1.text = OTApp.getString(R.string.time_range_picker_1_day)
                ui_button_preset_2.text = OTApp.getString(R.string.time_range_picker_1_week)
            } else if (granularity == Granularity.TIME) {
                ui_button_preset_1.text = OTApp.getString(R.string.time_range_picker_30_mins)
                ui_button_preset_2.text = OTApp.getString(R.string.time_range_picker_1_hour)
            }
        }
    }


    override fun onClick(button: View) {
        if (button === fromButton || button === toButton) {
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

                } else {
                    dateTimeDialogPicker.value = cal.timeInMillis
                    dateTimeDialogPicker.tag = if (button === fromButton) {
                        "from"
                    } else if (button === toButton) {
                        "to"
                    } else null

                    if (!datetimeDialog.isShowing) {
                        datetimeDialog.show()
                    }
                }
            }
        } else if (button === ui_button_preset_1) {
            when (granularity) {
                Granularity.DATE -> {
                    to += TimeHelper.daysInMilli * 1
                }
                Granularity.TIME -> {
                    to += TimeHelper.minutesInMilli * 30
                }
            }
            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
        } else if (button === ui_button_preset_2) {
            when (granularity) {
                Granularity.DATE -> {
                    to += TimeHelper.daysInMilli * 7
                }
                Granularity.TIME -> {
                    to += TimeHelper.hoursInMilli * 1
                }
            }
            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
        } else if (button === ui_button_preset_now) {
            val beforeFrom = from
            val beforeTo = to
            to = System.currentTimeMillis()

            if (beforeFrom != from || beforeTo != to)
                timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
        } else if (button === ui_button_up) {
            val beforeFrom = from
            val beforeTo = to
            from = to

            if (beforeFrom != from || beforeTo != to)
                timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
        } else if (button === ui_button_down) {
            val beforeFrom = from
            val beforeTo = to
            to = from

            if (beforeFrom != from || beforeTo != to)
                timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
        }
    }
}