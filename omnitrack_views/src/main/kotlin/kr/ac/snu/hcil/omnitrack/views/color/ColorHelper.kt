package kr.ac.snu.hcil.omnitrack.views.color

import android.content.Context
import android.graphics.Color
import kr.ac.snu.hcil.omnitrack.views.R

object ColorHelper {

    private var trackerPalette: IntArray? = null

    fun getTrackerColorPalette(context: Context): IntArray {
        if (trackerPalette == null) {
            trackerPalette = context.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
        }
        return trackerPalette!!
    }
}