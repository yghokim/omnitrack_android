package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.widget.CalendarView
import kr.ac.snu.hcil.omnitrack.R
import java.util.*

/**
 * Created by Young-Ho on 8/7/2016.
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

        val cal = GregorianCalendar(TimeZone.getDefault())
        cal.set(year, month, day)

        val calendarView = CalendarView(context)
        calendarView.setDate(cal.timeInMillis, false, true)

        calendarView.setOnDateChangeListener(object : CalendarView.OnDateChangeListener {
            override fun onSelectedDayChange(p0: CalendarView, p1: Int, p2: Int, p3: Int) {
                year = p1
                month = p2
                day = p3
            }
        })

        return AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.msg_pick_date))
                .setView(calendarView)
                .setPositiveButton(R.string.msg_ok) { a, b ->
                    val cal = GregorianCalendar(TimeZone.getDefault())
                    cal.set(year, month, day)
                    listener?.invoke(cal.timeInMillis, year, month, day)
                }
                .setNegativeButton(R.string.msg_cancel, null)
                .create()
    }


}