package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_attached_tracker_list_element.view.*
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.backgroundDrawable

/**
 * Created by younghokim on 2017. 10. 26..
 */
open class AttachedTrackerViewHolder(parent: ViewGroup?, @LayoutRes layoutId: Int = R.layout.layout_attached_tracker_list_element) : RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(layoutId, parent, false)) {
    private var indicatorLayer: Drawable? = null

    init {
        val mutated = itemView.backgroundDrawable?.mutate()
        mutated?.let {
            if (it is LayerDrawable) {
                indicatorLayer = it.findDrawableByLayerId(R.id.layer_color_indicator)
            }
        }
        itemView.background = mutated
    }

    fun setColor(color: Int) {
        indicatorLayer?.let { DrawableCompat.setTint(it, color) }
    }

    fun setName(name: String) {
        itemView.text.text = name
    }
}