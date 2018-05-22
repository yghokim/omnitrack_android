package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.utils.dipSize
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho Kim on 2016-07-19
 */
class ColorPaletteView(context: Context, attrs: AttributeSet?, defStyle: Int) : RecyclerView(context, attrs, defStyle) {

    private var selectedIndex: Int = 0

    private var buttonSize: Int = 0

    val colorChanged = Event<Int>()

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    init {
        //buttonSize = //context.resources.getDimensionPixelSize(R.dimen.color_selection_button_size)

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        adapter = Adapter()

        addItemDecoration(OffsetDecoration())

    }

    var selectedColor: Int
        get() {
            return OTApp.instance.colorPalette[selectedIndex]
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
        return OTApp.instance.colorPalette.indexOf(color)
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = ColorSelectionButton(context)
            buttonSize = ((parent.measuredWidth - parent.paddingLeft - parent.paddingRight) / itemCount - dipSize(4)).toInt()
            view.layoutParams = LayoutParams(buttonSize, buttonSize)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount(): Int {
            return OTApp.instance.colorPalette.size
        }

        override fun getItemId(position: Int): Long {
            return OTApp.instance.colorPalette[position].toLong()
        }


        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {


            init {
                view.setOnClickListener {
                    if (adapterPosition != selectedIndex) {
                        selectedIndex = adapterPosition
                        colorChanged.invoke(parent, selectedIndex)
                    }

                    TransitionManager.beginDelayedTransition(this@ColorPaletteView)
                    notifyDataSetChanged()
                }
            }

            fun bind(position: Int) {
                (view as ColorSelectionButton).color = OTApp.instance.colorPalette[position]
                view.isSelected = selectedIndex == position
            }
        }
    }

    inner class OffsetDecoration : ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State?) {
            super.getItemOffsets(outRect, view, parent, state)

            if (OTApp.instance.colorPalette.count() > 1) {

                val margin = (parent.measuredWidth - parent.paddingLeft - parent.paddingRight - OTApp.instance.colorPalette.size * buttonSize) / (OTApp.instance.colorPalette.size - 1)

                if (parent.getChildAdapterPosition(view) != 0) {
                    outRect.left = margin
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val thisState = SavedState(superState)
        thisState.selectedIndex = selectedIndex
        return thisState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val thisState = state as SavedState
        super.onRestoreInstanceState(thisState.superState)
        selectedIndex = thisState.selectedIndex
    }

    class SavedState : BaseSavedState {

        var selectedIndex: Int = 0

        constructor(source: Parcel) : super(source) {
            selectedIndex = source.readInt()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectedIndex)
        }

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