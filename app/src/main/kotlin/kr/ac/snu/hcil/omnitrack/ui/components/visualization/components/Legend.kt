package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.IDrawer
import java.util.*

/**
 * Created by Young-Ho on 9/10/2016.
 */
class Legend : IDrawer {


    val entries = ArrayList<Pair<String, Int>>()

    private var left: Float = 0f

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    val rectSize: Float = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_legend_rect_width)
    val rectMargin: Float = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_legend_label_spacing)
    val textSize: Float = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_legend_label_size)
    val spacing = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_legend_entry_spacing)

    private val topMargin = OTApp.instance.resourcesWrapped.getDimension(R.dimen.visi_legend_margin)

    private val rectBound = RectF()

    private val measureRect = Rect()


    var attachedTo: RectF = RectF()
        set(value) {
            if (field != value) {
                field = value

                refresh()
            }
        }

    init {
        labelPaint.textSize = OTApp.instance.resourcesWrapped.getDimension(R.dimen.vis_legend_label_size)
        labelPaint.isFakeBoldText = true
        labelPaint.color = ContextCompat.getColor(OTApp.instance, R.color.textColorMid)
    }

    fun refresh() {
        var intrinsicWidth = 0f

        for (entry in entries) {
            intrinsicWidth += rectSize + rectMargin
            labelPaint.getTextBounds(entry.first, 0, entry.first.length, measureRect)
            intrinsicWidth += measureRect.width() + spacing
        }

        left = attachedTo.left + (attachedTo.width() - intrinsicWidth) / 2
    }

    override fun onDraw(canvas: Canvas) {

        val top = attachedTo.bottom + topMargin

        var currentLeft = left
        for (entry in entries) {
            rectPaint.color = entry.second
            rectBound.set(currentLeft, top + topMargin, currentLeft + rectSize, top + topMargin + rectSize)
            canvas.drawRoundRect(rectBound, 5f, 5f, rectPaint)

            currentLeft += rectSize + rectMargin
            canvas.drawText(entry.first, currentLeft, top + topMargin + rectSize / 2 + textSize / 2, labelPaint)

            labelPaint.getTextBounds(entry.first, 0, entry.first.length, measureRect)
            currentLeft += measureRect.width() + spacing
        }

    }
}