package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.util.*

/**
 * Created by younghokim on 16. 9. 10..
 */
class PolyLineElement<T> : ADataEncodedDrawer<T> {

    private var _points = ArrayList<Float>()

    var linePaint: Paint
    var markerPaint: Paint

    var path  = Path()

    var color: Int = Color.DKGRAY
    var thickness: Float = 40f

    var markerRadius: Float = 40f
    var markerThickness: Float = 2f

    var drawMarker: Boolean = false
    var drawLine: Boolean = true

    var baseline: Float = 0f

    var drawGradient: Boolean = false

    fun getX(index: Int): Float {
        return _points[index * 2]
    }

    fun getY(index: Int): Float {
        return _points[index * 2 + 1]
    }

    fun setX(index: Int, x: Float) {
        _points[index * 2] = x
    }

    fun setY(index: Int, y: Float) {
        _points[index * 2 + 1] = y
    }


    fun set(index: Int, x: Float, y: Float) {
        setX(index, x)
        setY(index, y)
    }

    fun clearPoints() {
        _points.clear()
    }


    val numPoints: Int get() = _points.size / 2

    constructor(paint: Paint, markerPaint: Paint) {
        linePaint = paint
        this.markerPaint = markerPaint
    }

    constructor() {
        linePaint = Paint()
        markerPaint = Paint()
    }

    fun addPoint(x: Float, y: Float) {
        _points.add(x)
        _points.add(y)
    }

    fun fitNumPoints(count: Int) {
        val countDiff = count*2 - _points.size
        if (countDiff > 0) {
            for (i in 0..countDiff - 1) {
                _points.add(0f)
            }
        } else {
            for (i in 0..(-countDiff - 1)) {
                _points.removeAt(0)

            }
        }
    }

    fun refreshPath(){
        path=  Path()
        if(numPoints>1) {
            println("num points: ${numPoints}")
            path.moveTo(getX(0), getY(0))

            for(i in 1..numPoints-1)
            {
                path.lineTo(getX(i), getY(i))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        linePaint.color = color
        linePaint.strokeWidth = thickness

        if(drawLine && numPoints > 1)
            canvas.drawPath(path, linePaint)

        if(drawMarker)
        {
            markerPaint.strokeWidth = markerThickness
            markerPaint.style = Paint.Style.FILL
            markerPaint.color = Color.WHITE
            for(i in 0..numPoints-1)
            {

                canvas.drawCircle(getX(i), getY(i), markerRadius, markerPaint)
            }

            markerPaint.style = Paint.Style.STROKE
            markerPaint.color = color
            for(i in 0..numPoints-1)
            {
                canvas.drawCircle(getX(i), getY(i), markerRadius, markerPaint)
            }
        }

        //canvas.drawLines(_points.toFloatArray(), linePaint)
    }

}