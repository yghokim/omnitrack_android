package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-19.
 */
class ColorPaletteView(context: Context, attrs: AttributeSet?, defStyle: Int) : RecyclerView(context, attrs, defStyle) {

    private lateinit var colors : IntArray

    private var selectedIndex : Int = 0

    private var buttonSize : Int = 0

    val colorChanged = Event<Int>()

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    init{

        val colorStringArray = resources.getStringArray(R.array.colorPaletteArray)

        colors = colorStringArray.map{ Color.parseColor(it) }.toIntArray()

        buttonSize = context.resources.getDimensionPixelSize(R.dimen.color_selection_button_size)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        adapter = Adapter()

        addItemDecoration(OffsetDecoration())
    }

    var selectedColor: Int
        get() {
            return colors[selectedIndex]
        }
        set(value) {
            val index = findColorIndex(value)
            if (index >= 0) {
                if (index != selectedIndex) {
                    val oldSelected = selectedIndex
                    selectedIndex = index
                    adapter.notifyItemChanged(selectedIndex)
                    adapter.notifyItemChanged(oldSelected)
                }
            }
        }

    fun findColorIndex(color: Int): Int {
        return colors.indexOf(color)
    }

    inner class Adapter() : RecyclerView.Adapter<Adapter.ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = ColorSelectionButton(context)
            view.layoutParams = LayoutParams(buttonSize, buttonSize)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int {
            return colors.count()
        }

        override fun getItemId(position: Int): Long {
            return colors[position] as Long;
        }


        inner class ViewHolder(val view : View) : RecyclerView.ViewHolder(view){


            init{
                view.setOnClickListener {
                    if(adapterPosition != selectedIndex)
                    {
                        val toDeselect = selectedIndex
                        selectedIndex = adapterPosition
                        notifyItemChanged(toDeselect)
                        notifyItemChanged(selectedIndex)
                        colorChanged.invoke(parent, selectedIndex)
                    }
                    else{
                        notifyItemChanged(selectedIndex)
                    }
                }
            }

            fun bind(position: Int){
                (view as ColorSelectionButton).color = colors[position]
                (view as ColorSelectionButton).isChecked = selectedIndex == position
            }
        }
    }

    inner class OffsetDecoration: RecyclerView.ItemDecoration(){
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State?) {
            super.getItemOffsets(outRect, view, parent, state)

            if(colors.count() > 1) {

                val margin = (parent.measuredWidth - parent.paddingLeft - parent.paddingRight - colors.count()  * buttonSize) / (colors.count()  - 1)

                if (parent.getChildAdapterPosition(view) != 0) {
                    outRect.left = margin
                }
            }
        }
    }
}