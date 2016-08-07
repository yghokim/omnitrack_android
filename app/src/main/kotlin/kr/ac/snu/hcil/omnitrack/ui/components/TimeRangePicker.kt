package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.CalendarPickerDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.DateTimePickerDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
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

    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var durationIndicator: TextView

    private var from: Long = System.currentTimeMillis()
        set(value) {
            val processed: Long
            if (granularity == Granularity.DATE) {
                processed = TimeHelper.cutTimePartFromEpoch(value)
            } else {
                processed = value
            }

            if (field != processed) {
                field = processed

                suspendInvalidate = true
                to = Math.max(processed, to)
                suspendInvalidate = false
                invalidateViewSettings()
            }
        }


    private var to: Long = System.currentTimeMillis()
        set(value) {
            val processed: Long
            if (granularity == Granularity.DATE) {
                processed = TimeHelper.cutTimePartFromEpoch(value)
            } else {
                processed = value
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
        return TimeSpan(from, to)
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
                context.resources.getString(R.string.dateformat_full)
        }

        format = SimpleDateFormat(formatString)

        if (granularity == Granularity.DATE) {
            from = TimeHelper.cutTimePartFromEpoch(from)
            to = TimeHelper.cutTimePartFromEpoch(to)
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

                if (granularity == Granularity.DATE) {
                    //date picking

                    val cal = GregorianCalendar(TimeZone.getDefault())
                    cal.timeInMillis = timestamp

                    CalendarPickerDialogFragment.getInstance(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).showDialog(activity.supportFragmentManager) {
                        timestamp: Long, year: Int, month: Int, day: Int ->
                        if (button === fromButton) {
                            from = timestamp
                        } else if (button === toButton) {
                            to = timestamp
                        }
                    }
                } else {
                    //datetime picking
                    DateTimePickerDialogFragment.getInstance(timestamp).showDialog(activity.supportFragmentManager) {
                        timestamp ->
                        if (button === fromButton) {
                            from = timestamp
                        } else if (button === toButton) {
                            to = timestamp
                        }
                    }
                }
            }
        }
    }
}