package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.AppCompatButton
import android.view.Gravity
import android.widget.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.*
import java.util.*

/**
 * Created by Young-Ho on 8/7/2016.
 */
class DateTimePickerDialogFragment : DialogFragment() {

    companion object {
        private fun setDateLabel(tabHost: TabHost, year: Int, zeroBasedMonth: Int, day: Int) {
            val labelView: TextView = tabHost.tabWidget.getChildAt(0).findViewById(android.R.id.title)
            labelView.text = "${year}-${zeroBasedMonth + 1}-${day}"
        }

        private fun setTimeLabel(tabHost: TabHost, hourOfDay: Int, minute: Int) {
            val labelView: TextView = tabHost.tabWidget.getChildAt(1).findViewById(android.R.id.title)
            if (hourOfDay == 12 && minute == 0) {
                //PM 12:00 : Noon
                labelView.setText(R.string.msg_noon)
            } else {
                var hour = hourOfDay % 12
                if (hour == 0) hour = 12
                labelView.text = "${if (hourOfDay < 12) {
                    "AM"
                } else {
                    "PM"
                }} ${String.format("%02d", hour)}:${String.format("%02d", minute)}"
            }
        }

        fun getInstance(timestamp: Long): DateTimePickerDialogFragment {
            val bundle = Bundle()
            bundle.putLong("timestamp", timestamp)

            val fragment = DateTimePickerDialogFragment()
            fragment.arguments = bundle
            return fragment
        }

        const val TAB_DATE = "DateTab"
        const val TAB_TIME = "TimeTab"
    }

    private lateinit var tabHost: TabHost

    private var listener: ((timestamp: Long) -> Unit)? = null

    private var year: Int = 0
    private var zeroBasedMonth: Int = 0
    private var day: Int = 0

    private fun applyModeToButton(tabTag: String, modeButton: AppCompatButton) {
        when (tabTag) {
            TAB_DATE -> {
                modeButton.setText(R.string.msg_pick_time)
                (modeButton.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
                modeButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.right_dark, 0)
            }
            TAB_TIME -> {
                modeButton.setText(R.string.msg_pick_date)
                (modeButton.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
                modeButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.left_dark, 0, 0, 0)
            }
        }
    }

    fun showDialog(fragmentManager: FragmentManager, listener: (timestamp: Long) -> Unit) {
        this.listener = listener
        show(fragmentManager, "dialog")
    }

    private var currentTab: String
        get() {
            return tabHost.currentTabTag!!
        }
        set(value) {
            tabHost.setCurrentTabByTag(value)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_datetime_picker, null)

        tabHost = view.findViewById(R.id.tabHost)
        tabHost.setup()

        val modeButton = view.findViewById<AppCompatButton>(R.id.ui_mode_button).apply {
            InterfaceHelper.removeButtonTextDecoration(this)
            applyModeToButton(TAB_DATE, this)
            setOnClickListener {
                currentTab = if (currentTab == TAB_DATE) {
                    TAB_TIME
                } else TAB_DATE
            }
        }

        tabHost.addTab(
                tabHost.newTabSpec(TAB_DATE).setContent(R.id.ui_calendar_tab_content).setIndicator("Date")
        )

        tabHost.addTab(
                tabHost.newTabSpec(TAB_TIME).setContent(R.id.ui_time_tab_content).setIndicator("Time")
        )

        tabHost.setOnTabChangedListener { tabName ->
            applyModeToButton(tabName, modeButton)
        }

        val calendar = GregorianCalendar(TimeZone.getDefault())
        calendar.timeInMillis = arguments?.getLong("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
        calendar.set(Calendar.MILLISECOND, 0)

        year = calendar.getYear()
        zeroBasedMonth = calendar.getZeroBasedMonth()
        day = calendar.getDayOfMonth()

        //apply labels
        setDateLabel(tabHost, year, zeroBasedMonth, day)
        setTimeLabel(tabHost, calendar.getHourOfDay(), calendar.getMinute())

        val calendarView: CalendarView = view.findViewById(R.id.ui_calendar_view)

        calendarView.setDate(calendar.timeInMillis, false, true)

        calendarView.setOnDateChangeListener { _, y, m, d ->
            setDateLabel(tabHost, y, m, d)
            year = y
            zeroBasedMonth = m
            day = d
        }

        val timePickerView: TimePicker = view.findViewById(R.id.ui_time_picker_view)
        timePickerView.setHourOfDayCompat(calendar.getHourOfDay())
        timePickerView.setMinuteCompat(calendar.getMinute())

        timePickerView.setOnTimeChangedListener { picker, hourOfDay, minute ->

            setTimeLabel(tabHost, hourOfDay, minute)
        }

        return AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.msg_pick_date_and_time))
                .setView(view)
                .setPositiveButton(R.string.msg_ok) { a, b ->
                    val cal = GregorianCalendar(TimeZone.getDefault())
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(year, zeroBasedMonth, day, timePickerView.getHourOfDayCompat(), timePickerView.getMinuteCompat())
                    listener?.invoke(cal.timeInMillis)
                }
                .setNegativeButton(R.string.msg_cancel, null)
                .create()
    }
}