package kr.ac.snu.hcil.android.common.view.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.CalendarView
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kr.ac.snu.hcil.android.common.view.R
import java.util.*

/**
 * Created by Young-Ho Kim on 8/7/2016
 */
class CalendarPickerDialogFragment : DialogFragment() {

    companion object {

        const val YEAR = "year"
        const val MONTH = "month"
        const val DAY = "day"


        fun getInstance(year: Int, zeroBasedMonth: Int, day: Int): CalendarPickerDialogFragment {
            val bundle = Bundle()
            bundle.putInt(YEAR, year)
            bundle.putInt(MONTH, zeroBasedMonth)
            bundle.putInt(DAY, day)

            val fragment = CalendarPickerDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private var listener: ((timestamp: Long, year: Int, month: Int, day: Int) -> Unit)? = null

    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0

    fun showDialog(fragmentManager: FragmentManager, listener: (timestamp: Long, year: Int, month: Int, day: Int) -> Unit) {
        this.listener = listener
        show(fragmentManager, "dialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val now = Calendar.getInstance()
        year = arguments?.getInt(YEAR, now.get(Calendar.YEAR)) ?: now.get(Calendar.YEAR)
        month = arguments?.getInt(MONTH, now.get(Calendar.MONTH)) ?: now.get(Calendar.MONTH)
        day = arguments?.getInt(DAY, now.get(Calendar.DAY_OF_MONTH)) ?: now.get(Calendar.DAY_OF_MONTH)


        val dialogBuilder = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.msg_ok) { _, _ ->
                    val calendar = GregorianCalendar(TimeZone.getDefault())
                    calendar.set(year, month, day)
                    listener?.invoke(calendar.timeInMillis, year, month, day)
                }
                .setNegativeButton(R.string.msg_cancel, null)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val datePicker = DatePicker(context)

            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                datePicker.calendarViewShown = false
                datePicker.spinnersShown = true
            }

            datePicker.init(year, month, day, object : DatePicker.OnDateChangedListener {
                override fun onDateChanged(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
                    this@CalendarPickerDialogFragment.year = year
                    this@CalendarPickerDialogFragment.month = monthOfYear
                    this@CalendarPickerDialogFragment.day = dayOfMonth
                }

            })
            dialogBuilder
                    .setPositiveButton(R.string.msg_ok) { _, _ ->
                        year = datePicker.year
                        month = datePicker.month
                        day = datePicker.dayOfMonth
                        val calendar = GregorianCalendar(TimeZone.getDefault())
                        calendar.set(year, month, day)
                        listener?.invoke(calendar.timeInMillis, year, month, day)
                    }.setView(datePicker)
        } else {

            val cal = GregorianCalendar(TimeZone.getDefault())
            cal.set(year, month, day)

            val calendarView = CalendarView(requireActivity())
            calendarView.setDate(cal.timeInMillis, false, true)

            calendarView.setOnDateChangeListener { _, p1, p2, p3 ->
                year = p1
                month = p2
                day = p3
            }
            dialogBuilder
                    .setTitle(resources.getString(R.string.msg_pick_date))
                    .setView(calendarView)
        }

        return dialogBuilder.create()
    }


}