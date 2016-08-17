package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import org.apmem.tools.layouts.FlowLayout
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-08-17.
 */
open class WordListView : FlowLayout {

    var words: Array<String> by Delegates.observable(arrayOf()) {
        prop, old, new ->
        refresh()
    }

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

    private fun refresh() {
        val numChildViewToAdd = words.size - childCount
        if (numChildViewToAdd < 0) {
            removeViews(childCount + numChildViewToAdd, -numChildViewToAdd)
        } else if (numChildViewToAdd > 0) {

            for (i in 0..numChildViewToAdd - 1) {
                addView(makeChildView())
            }
        }

        for (i in 0..words.size - 1) {
            (getChildAt(i) as TextView).text = words[i]
        }
    }

    protected open fun makeChildView(): TextView {
        val view = TextView(context)

        InterfaceHelper.setTextAppearance(view, textAppearanceId)

        view.setBackgroundResource(R.drawable.word_list_element_frame)

        val lp = FlowLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = 20
        view.layoutParams = lp

        return view
    }
}