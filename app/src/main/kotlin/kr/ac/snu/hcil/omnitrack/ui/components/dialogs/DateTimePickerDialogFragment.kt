package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.widget.CalendarView
import android.widget.NumberPicker
import android.widget.TabHost
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import java.util.*

/**
 * Created by Young-Ho on 8/7/2016.
 */
class DateTimePickerDialogFragment : DialogFragment() {

    companion object {
        private fun setDateLabel(tabHost: TabHost, year: Int, zeroBasedMonth: Int, day: Int) {
            val labelView = tabHost.tabWidget.getChildAt(0).findViewById(android.R.id.title) as TextView
            labelView.text = "${year}-${zeroBasedMonth + 1}-${day}"
        }

        fun getInstance(timestamp: Long): DateTimePickerDialogFragment {
            val bundle = Bundle()
            bundle.putLong("timestamp", timestamp)

            val fragment = DateTimePickerDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private var listener: ((timestamp: Long) -> Unit)? = null

    private var year: Int = 0
    private var zeroBasedMonth: Int = 0
    private var day: Int = 0

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var ampmPicker: NumberPicker

    fun showDialog(fragmentManager: FragmentManager, listener: (timestamp: Long) -> Unit) {
        this.listener = listener
        show(fragmentManager, "dialog")
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.dialog_datetime_picker, null)

        val tabHost = view.findViewById(R.id.tabHost) as TabHost
        tabHost.setup()

        tabHost.addTab(
                tabHost.newTabSpec("DateTab").setContent(R.id.ui_calendar_tab_content).setIndicator("Date")
        )

        tabHost.addTab(
                tabHost.newTabSpec("TimeTab").setContent(R.id.ui_time_tab_content).setIndicator("Time")
        )


        val calendar = GregorianCalendar(TimeZone.getDefault())
        calendar.timeInMillis = arguments?.getLong("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
        calendar.set(Calendar.MILLISECOND, 0)

        year = calendar.get(Calendar.YEAR)
        zeroBasedMonth = calendar.get(Calendar.MONTH)
        day = calendar.get(Calendar.DAY_OF_MONTH)

        setDateLabel(tabHost, year, zeroBasedMonth, day)

        val calendarView = view.findViewById(R.id.ui_calendar_view) as CalendarView

        calendarView.setDate(calendar.timeInMillis, false, true)

        calendarView.setOnDateChangeListener { calendarView, y, m, d ->
            setDateLabel(tabHost, y, m, d)
            year = y
            zeroBasedMonth = m
            day = d
        }

        hourPicker = view.findViewById(R.id.ui_hour_picker) as NumberPicker
        minutePicker = view.findViewById(R.id.ui_minute_picker) as NumberPicker
        secondPicker = view.findViewById(R.id.ui_second_picker) as NumberPicker
        ampmPicker = view.findViewById(R.id.ui_ampm_picker) as NumberPicker

        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        val hour = calendar.get(Calendar.HOUR)
        hourPicker.value = if (hour == 0) {
            12
        } else {
            hour
        }

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.displayedValues = Array<String>(60) {
            i ->
            String.format("%02d", i);
        }
        minutePicker.value = calendar.get(Calendar.MINUTE)

        secondPicker.minValue = 0
        secondPicker.maxValue = 59
        secondPicker.displayedValues = Array<String>(60) {
            i ->
            String.format("%02d", i);
        }

        secondPicker.value = calendar.get(Calendar.SECOND)

        ampmPicker.minValue = 0
        ampmPicker.maxValue = 1
        ampmPicker.displayedValues = arrayOf("AM", "PM")
        ampmPicker.value = calendar.get(Calendar.AM_PM)


        return AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.msg_pick_date))
                .setView(view)
                .setPositiveButton(R.string.msg_ok) { a, b ->
                    val cal = GregorianCalendar(TimeZone.getDefault())
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(year, zeroBasedMonth, day, (hourPicker.value % 12) + 12 * ampmPicker.value, minutePicker.value, secondPicker.value)
                    listener?.invoke(cal.timeInMillis)
                }
                .setNegativeButton(R.string.msg_cancel, null)
                .create()
    }
}