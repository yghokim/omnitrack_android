package kr.ac.snu.hcil.omnitrack.ui.components.common.time

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.android.synthetic.main.component_time_range_picker.view.*
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.getActivity
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 8/7/2016.
 */
class TimeRangePicker : ConstraintLayout, View.OnClickListener {

    enum class Granularity {
        DATE, TIME
    }

    private var suspendInvalidate: Boolean = false

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

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        inflateContent(R.layout.component_time_range_picker, true)

        ui_button_from.setOnClickListener(this)
        ui_button_to.setOnClickListener(this)

        ui_button_preset_1.setOnClickListener(this)
        ui_button_preset_2.setOnClickListener(this)
        ui_button_preset_now.setOnClickListener(this)

        ui_button_up.setOnClickListener(this)
        ui_button_down.setOnClickListener(this)

        InterfaceHelper.removeButtonTextDecoration(ui_button_from)
        InterfaceHelper.removeButtonTextDecoration(ui_button_to)

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
            ui_button_from.text = format.format(Date(from))
            ui_button_to.text = format.format(Date(to))
            ui_interval_indicator.text = TimeHelper.durationToText(to - from, true, context)

            if (granularity == Granularity.DATE) {
                ui_button_preset_1.text = resources.getString(R.string.time_range_picker_1_day)
                ui_button_preset_2.text = resources.getString(R.string.time_range_picker_1_week)
            } else if (granularity == Granularity.TIME) {
                ui_button_preset_1.text = resources.getString(R.string.time_range_picker_30_mins)
                ui_button_preset_2.text = resources.getString(R.string.time_range_picker_1_hour)
            }
        }
    }


    override fun onClick(button: View) {
        if (button === ui_button_from || button === ui_button_to) {
            val activity = getActivity()
            if (activity != null) {

                val timestamp = if (button === ui_button_from) {
                    from
                } else if (button === ui_button_to) {
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

                        if (button === ui_button_from) {
                            from = timestamp
                        } else if (button === ui_button_to) {
                            to = timestamp
                        }

                        if (beforeFrom != from || beforeTo != to) {
                            timeRangeChanged.invoke(this@TimeRangePicker, getTimeSpan())
                        }
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                            .show(activity.supportFragmentManager, "DatePickerDialog")

                } else {
                    dateTimeDialogPicker.value = cal.timeInMillis
                    dateTimeDialogPicker.tag = if (button === ui_button_from) {
                        "from"
                    } else if (button === ui_button_to) {
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