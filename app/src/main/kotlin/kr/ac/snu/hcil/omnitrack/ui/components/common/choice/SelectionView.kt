package kr.ac.snu.hcil.omnitrack.ui.components.common.choice

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.support.annotation.ArrayRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class SelectionView(context: Context, attrs: AttributeSet?, defStyle: Int) : RecyclerView(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private val values = ArrayList<String>()
    private var buttonPadding: Rect
    private var buttonTextSize: Float = 10.0f
    private var selectedTextColor: Int = Color.WHITE
    private var unSelectedTextColor: Int = Color.BLACK


    var selectedIndex: Int by Delegates.observable(0) {
        prop, old, new ->
        if (old != new) {
            println("selected Index changed - $new")
            adapter.notifyItemChanged(old)
            adapter.notifyItemChanged(new)
            onSelectedIndexChanged.invoke(this, new)
        }
    }

    val selectedItem: String
        get()= values[selectedIndex]

    val onSelectedIndexChanged = Event<Int>()

    fun setValues(newValues: Array<String>) {
        values.clear()
        values.addAll(newValues)
        adapter.notifyDataSetChanged()
    }

    fun setValues(@ArrayRes newValuesRes: Int) {
        setValues(resources.getStringArray(newValuesRes))
    }

    init {
        overScrollMode = View.OVER_SCROLL_NEVER
        buttonPadding = Rect(resources.getDimensionPixelOffset(R.dimen.selection_view_button_padding_left),
                resources.getDimensionPixelOffset(R.dimen.selection_view_button_padding_top),
                resources.getDimensionPixelOffset(R.dimen.selection_view_button_padding_right),
                resources.getDimensionPixelOffset(R.dimen.selection_view_button_padding_bottom)
        )

        buttonTextSize = resources.getDimensionPixelSize(R.dimen.selection_view_button_text_size).toFloat()

        selectedTextColor = ContextCompat.getColor(context, R.color.selection_view_button_selected_text_color)
        unSelectedTextColor = ContextCompat.getColor(context, R.color.selection_view_button_unselected_text_color)


        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        addItemDecoration(SpaceItemDecoration(LinearLayoutManager.HORIZONTAL, resources.getDimensionPixelOffset(R.dimen.selection_view_button_spacing)))

        adapter = Adapter()

        values.addAll(arrayOf("Second", "Minute", "Hour", "Date"))
    }

    inner class Adapter() : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = TextView(context)
            view.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            view.minHeight = 0
            view.minWidth = 0
            view.setPadding(buttonPadding.left, buttonPadding.top, buttonPadding.right, buttonPadding.bottom)
            view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)

            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int {
            return values.count()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }


        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            var button: TextView

            init {
                button = view as TextView
                view.setOnClickListener {
                    selectedIndex = adapterPosition
                }
            }

            fun bind(position: Int) {

                button.text = values[position]
                if (selectedIndex == position) {
                    button.setBackgroundResource(R.drawable.capsule_frame)
                    button.setTextColor(selectedTextColor)
                } else {
                    button.setBackgroundResource(R.drawable.transparent_button_background)
                    button.setTextColor(unSelectedTextColor)
                }

                view.setPadding(buttonPadding.left, buttonPadding.top, buttonPadding.right, buttonPadding.bottom)
            }
        }
    }
}