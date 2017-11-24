package kr.ac.snu.hcil.omnitrack.ui.components.common.choice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import org.apmem.tools.layouts.FlowLayout
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-08-17.
 */
open class WordListView : FlowLayout {

    companion object {
        private var _colorPalette: IntArray? = null

        fun getColorPalette(context: Context): IntArray {
            if (_colorPalette == null) {
                _colorPalette = context.resources.getStringArray(R.array.choiceColorPaletteArray).map { Color.parseColor(it) }.toIntArray()
            }

            return _colorPalette!!
        }
    }

    var words: Array<String> by Delegates.observable(arrayOf()) {
        prop, old, new ->
        refresh()
    }

    var useColors = false

    val colorIndexList = ArrayList<Int>()

    var textAppearanceId: Int = R.style.tagTextAppearance
        set(value) {
            if (field != value) {
                field = value

                for (i in 0..childCount - 1) {
                    val c = getChildAt(i)
                    if (c is TextView) {
                        InterfaceHelper.setTextAppearance(c, textAppearanceId)
                    }
                }
            }
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(context, attributeSet, defStyle)

    fun refresh() {
        val numChildViewToAdd = words.size - childCount
        if (numChildViewToAdd < 0) {
            removeViews(childCount + numChildViewToAdd, -numChildViewToAdd)
        } else if (numChildViewToAdd > 0) {

            for (i in 0..numChildViewToAdd - 1) {
                addView(makeChildView(i))
            }
        }

        for (i in 0..words.size - 1) {
            val view = (getChildAt(i) as TextView)
            view.text = words[i]

            if (useColors) {
                val shape = (view.background as LayerDrawable).findDrawableByLayerId(R.id.layer_color_shape) as GradientDrawable
                shape.setColor(getColorPalette(context)[colorIndexList[i]])
            }

        }
    }

    protected open fun makeChildView(position: Int): TextView {
        val view = TextView(context)

        InterfaceHelper.setTextAppearance(view, textAppearanceId)

        view.setBackgroundResource(R.drawable.word_list_element_frame)

        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = 20
        view.layoutParams = lp

        return view
    }
}