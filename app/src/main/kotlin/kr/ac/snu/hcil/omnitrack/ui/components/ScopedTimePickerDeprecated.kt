package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import java.util.*

/**
 * Created by younghokim on 16. 7. 22..
 */
@Deprecated("")
class ScopedTimePickerDeprecated : RecyclerView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    companion object {
        const val YEAR = 0
        const val MONTH = 1
        const val DAY = 2
        const val HOUR = 3
        const val MINUTE = 4
        const val SECOND = 5
    }

    private var scopeStart = YEAR
    private var scopeEnd = SECOND

    private val calendar = Calendar.getInstance()

    fun setScope(start: Int, end: Int): Boolean {
        if (start >= end)
            return true
        else {
            scopeStart = start
            scopeEnd = end
            adapter.notifyDataSetChanged()
            return true
        }
    }

    init {
        super.setAdapter(Adapter())
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }


    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        //do nothing
    }

    inner class Adapter() : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = NumberPicker(context)
            view.layoutParams = ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(scopeStart + position)
        }

        override fun getItemCount(): Int {
            return scopeEnd - scopeStart + 1
        }

        override fun getItemId(position: Int): Long {
            return (scopeStart + position).toLong()
        }


        inner class ViewHolder(val digitPicker: NumberPicker) : RecyclerView.ViewHolder(digitPicker) {
            init {

            }

            fun bind(timeVariable: Int) {
                when (timeVariable) {
                    YEAR -> {
                        digitPicker.minValue = 1950
                        digitPicker.maxValue = 2050
                        digitPicker.value = calendar.get(Calendar.YEAR)
                    }
                    MONTH -> {
                        digitPicker.displayedValues = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        digitPicker.value = calendar.get(Calendar.MONTH)
                    }
                    DAY -> {
                        digitPicker.minValue = 1
                        digitPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        digitPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
                    }
                    HOUR -> {
                        digitPicker.minValue = 1
                        digitPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        digitPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
                    }
                    MINUTE -> {
                        digitPicker.minValue = 0
                        digitPicker.maxValue = 59
                        digitPicker.value = calendar.get(Calendar.MINUTE)
                    }
                    SECOND -> {
                        digitPicker.minValue = 0
                        digitPicker.maxValue = 59
                        digitPicker.value = calendar.get(Calendar.SECOND)
                    }
                }
            }
        }
    }

}