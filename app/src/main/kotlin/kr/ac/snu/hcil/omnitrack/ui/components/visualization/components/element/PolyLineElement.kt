package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.*

/**
 * Created by younghokim on 16. 9. 10..
 */
class PolyLineElement<T> : ADataEncodedDrawer<T> {

    private var _points = ArrayList<Float>()

    var linePaint: Paint

    var color: Int = Color.DKGRAY
    var thickness: Float = 3f

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

    constructor(paint: Paint) {
        linePaint = paint
    }

    constructor() {
        linePaint = Paint()
    }

    fun addPoint(x: Float, y: Float) {
        _points.add(x)
        _points.add(y)
    }

    fun fitNumPoints(count: Int) {
        val countDiff = count - _points.size
        if (countDiff > 0) {
            for (i in 0..countDiff - 1) {
                _points.add(0f)
                _points.add(0f)
            }
        } else {
            for (i in 0..-countDiff - 1) {
                _points.removeAt(_points.size - 1)
                _points.removeAt(_points.size - 1)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        linePaint.color = color
        linePaint.strokeWidth = thickness

        canvas.drawLines(_points.toFloatArray(), linePaint)
    }

}