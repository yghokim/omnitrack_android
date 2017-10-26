package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.LayoutRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
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
}