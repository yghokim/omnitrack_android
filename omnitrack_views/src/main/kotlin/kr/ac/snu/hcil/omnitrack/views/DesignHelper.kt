package kr.ac.snu.hcil.omnitrack.views

import android.content.Context
import android.graphics.Color

object DesignHelper {

    private var trackerPalette: IntArray? = null

    fun getTrackerColorPalette(context: Context): IntArray {
        if (trackerPalette == null) {
            trackerPalette = context.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
        }
        return trackerPalette!!
    }
}