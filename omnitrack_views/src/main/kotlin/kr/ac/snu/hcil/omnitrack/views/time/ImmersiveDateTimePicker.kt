package kr.ac.snu.hcil.omnitrack.views.time

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.shawnlin.numberpicker.NumberPicker
import kotlinx.android.synthetic.main.component_datetime_picker_immersive.view.*
import kr.ac.snu.hcil.android.common.time.getAmPm
import kr.ac.snu.hcil.android.common.time.getHour
import kr.ac.snu.hcil.android.common.time.getMinute
import kr.ac.snu.hcil.android.common.time.setHourOfDay
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.views.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Created by younghokim on 2017. 11. 30..
 */
class ImmersiveDateTimePicker : ConstraintLayout, NumberPicker.OnValueChangeListener {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    var value: Long
        get() {
            return calendar.timeInMillis
        }
        set(new) {
            if (new != calendar.timeInMillis) {
                calendar.timeInMillis = new
                updateViewsFromCalendar()
            }
        }

    private var calendar = Calendar.getInstance()
    private var dateFormat: DateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_dow_and_year))

    private var suspendValueChangedEvent = false

    private fun init(context: Context, attrs: AttributeSet?) {
        inflateContent(R.layout.component_datetime_picker_immersive, true)

        ui_picker_ampm.formatter = ListFormatter(listOf("AM", "PM"))

        ui_picker_hour.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                println("hourPicker touch up")
            }
            false
        }
        ui_picker_hour.setOnValueChangedListener(this)
        ui_picker_minute.setOnValueChangedListener(this)
        ui_picker_ampm.setOnValueChangedListener(this)

        ui_button_prev_date.setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            updateViewsFromCalendar()
        }
        ui_button_next_date.setOnClickListener {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            updateViewsFromCalendar()
        }


        updateViewsFromCalendar()
    }

    fun setOnApplyButtonClickedListener(listener: OnClickListener) {
        ui_button_ok.setOnClickListener(listener)
    }

    private fun updateViewsFromCalendar() {
        suspendValueChangedEvent = true

        val hour = calendar.getHour()

        ui_picker_hour.value = if (hour == 0) 12 else hour
        ui_picker_minute.value = calendar.getMinute()
        ui_picker_ampm.value = calendar.getAmPm()
        ui_date_indicator.text = dateFormat.format(calendar.time)

        val now = System.currentTimeMillis()
        ui_date_semantic_indicator.text = DateUtils.getRelativeTimeSpanString(value, now, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_NO_YEAR)

        ui_relative_from_now_indicator.text = DateUtils.getRelativeTimeSpanString(value, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)

        suspendValueChangedEvent = false
    }


    override fun onValueChange(picker: NumberPicker, old: Int, new: Int) {
        if (!suspendValueChangedEvent) {
            when (picker) {
                ui_picker_hour -> {
                    calendar.setHourOfDay(new % 12 + 12 * ui_picker_ampm.value)
                }
                ui_picker_minute -> {
                    calendar.set(Calendar.MINUTE, new)
                }
                ui_picker_ampm -> {
                    calendar.setHourOfDay(ui_picker_hour.value % 12 + 12 * new)
                }
            }
            updateViewsFromCalendar()
        }
    }


    class ListFormatter(val list: List<String>) : NumberPicker.Formatter {
        override fun format(value: Int): String {
            return list[abs(value) % list.size]
        }

    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val thisState = SavedState(superState)
        thisState.timeMillis = calendar.timeInMillis
        return thisState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val thisState = state as SavedState
        super.onRestoreInstanceState(state)
        calendar.timeInMillis = thisState.timeMillis
        updateViewsFromCalendar()
    }

    class SavedState : BaseSavedState {

        var timeMillis: Long = 0

        constructor(source: Parcel) : super(source) {
            timeMillis = source.readLong()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeLong(timeMillis)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}