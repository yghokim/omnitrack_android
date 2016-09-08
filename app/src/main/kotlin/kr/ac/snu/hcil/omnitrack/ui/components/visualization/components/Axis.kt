package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.support.v4.graphics.ColorUtils
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.IDrawer

/**
 * Created by Young-Ho on 9/8/2016.
 */
class Axis(var pivot: Pivot): IDrawer {

    enum class Pivot{
        LEFT, BOTTOM
    }

    var drawPin: Boolean = false
    var drawBar: Boolean = true
    var drawGridLines: Boolean = false

    var linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var labelSpacing: Float = 0f

    var attachedTo: RectF = RectF()
        set(value)
        {
            if(field != value)
            {
                field = value
                when(pivot){
                    Pivot.BOTTOM-> scale?.setRealCoordRange(value.left, value.right)
                    Pivot.LEFT-> scale?.setRealCoordRange(value.bottom, value.top)
                }
            }
        }

    var scale: IAxisScale? = null

    private val tickLabelSizeMeasureRect = Rect()

    init{
        linePaint.color = OTApplication.app.resources.getColor(R.color.vis_color_axis, null)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = OTApplication.app.resources.getDimension(R.dimen.vis_axis_thickness)

        gridLinePaint.color = ColorUtils.setAlphaComponent(linePaint.color, 50)
        gridLinePaint.style = Paint.Style.STROKE
        gridLinePaint.strokeWidth = OTApplication.app.resources.getDimension(R.dimen.vis_axis_grid_thickness)

        labelPaint.style = Paint.Style.FILL
        labelPaint.color = OTApplication.app.resources.getColor(R.color.textColorMid, null)
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_categorical_size)

        labelSpacing = OTApplication.app.resources.getDimension(R.dimen.vis_axis_label_spacing)
    }

    override fun onDraw(canvas: Canvas) {

        when (pivot) {
            Pivot.BOTTOM -> labelPaint.textAlign = Paint.Align.CENTER
            Pivot.LEFT -> labelPaint.textAlign = Paint.Align.RIGHT
        }

        if (drawBar)
        {
            when (pivot) {
                Pivot.BOTTOM -> canvas.drawLine(attachedTo.left, attachedTo.bottom, attachedTo.right, attachedTo.bottom, linePaint)

                Pivot.LEFT -> canvas.drawLine(attachedTo.left, attachedTo.bottom, attachedTo.left, attachedTo.top, linePaint)
            }
        }

        for (i in 0..(scale?.numTicks ?: 0) - 1) {
            val tickCoord = scale?.getTickCoordAt(i) ?: 0f

            val tickLabel = scale?.getTickLabelAt(i)
            if (!tickLabel.isNullOrBlank()) {
                when (pivot) {
                    Pivot.BOTTOM ->
                        canvas.drawText(tickLabel, tickCoord, attachedTo.bottom + labelSpacing + labelPaint.textSize, labelPaint)
                    Pivot.LEFT -> {
                        labelPaint.getTextBounds(tickLabel, 0, tickLabel!!.length, tickLabelSizeMeasureRect)
                        canvas.drawText(tickLabel, attachedTo.left - labelSpacing, tickCoord + tickLabelSizeMeasureRect.height() / 2, labelPaint)
                    }
                }
            }

            if (drawGridLines) {
                when (pivot) {
                    Pivot.BOTTOM ->
                        canvas.drawLine(tickCoord, attachedTo.top, tickCoord, attachedTo.bottom, gridLinePaint)
                    Pivot.LEFT ->
                        canvas.drawLine(attachedTo.left, tickCoord, attachedTo.right, tickCoord, gridLinePaint)
                }
            }
        }

    }

}