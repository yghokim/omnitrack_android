package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeSpanAttribute
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.AttributeChartModel
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.Axis
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.ADataEncodedDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element.DataEncodedDrawingList
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.NumericScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.ATimelineChartDrawer
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import rx.Observable
import rx.functions.Func1
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho on 9/9/2016.
 */
class DurationTimelineModel(override val attribute: OTTimeSpanAttribute) : AttributeChartModel<DurationTimelineModel.AggregatedDuration>(attribute) {

    data class AggregatedDuration(val time: Long, val count: Int, val avgFrom: Float, val avgTo: Float, val earliest: Float = avgFrom, val latest: Float = avgTo)


    private var _isLoaded = false

    override val isLoaded: Boolean
        get() = _isLoaded

    private val data = ArrayList<AggregatedDuration>()

    private val subscriptions = CompositeSubscription()

    override fun onReload(finished: (Boolean) -> Unit) {

        subscriptions.clear()

        val xScale = QuantizedTimeScale()
        xScale.setDomain(getTimeScope().from, getTimeScope().to)
        xScale.quantize(currentGranularity)

        subscriptions.add(
                Observable.zip((0..xScale.numTicks - 1).map { xIndex ->

                    val from = xScale.binPointsOnDomain[xIndex]
                    val to = if (xIndex < xScale.numTicks - 1) xScale.binPointsOnDomain[xIndex + 1]
                    else getTimeScope().to

                    DatabaseManager.loadItems(attribute.tracker!!, TimeSpan.fromPoints(from, to)).flatMap<AggregatedDuration?>(Func1<List<OTItem>, Observable<AggregatedDuration?>> {
                        items ->
                        println("items during ${TimeSpan.fromPoints(from, to).toString()}; count: ${items.size}")
                        if (items.isNotEmpty()) {
                            val timeSpansCache = ArrayList<Pair<TimeSpan, Long>>()

                            items.forEach { item ->
                                println("item for ${item.timestampString}")
                                val v = item.getValueOf(attribute)
                                if (v is TimeSpan) {
                                    timeSpansCache.add(Pair(v, TimeHelper.cutTimePartFromEpoch(item.timestamp)))
                                }
                        }

                            println("timespans count: ${timeSpansCache.size}")
                            if (timeSpansCache.size > 0) {
                                val fromArray = timeSpansCache.map { timeToRatio(it.first.from, it.second) }
                                val toArray = timeSpansCache.map { timeToRatio(it.first.to, it.second) }

                                Observable.just(
                                        AggregatedDuration(
                                                from,
                                                timeSpansCache.size,
                                                fromArray.average().toFloat(),
                                                toArray.average().toFloat(),
                                                fromArray.min() ?: 0f,
                                                toArray.max() ?: 0f
                                        ))
                            } else Observable.just<AggregatedDuration>(null)
                        } else Observable.just(null)
                    })
                    //OTApplication.app.dbHelper.getItems(attribute.tracker!!, TimeSpan.fromPoints(from, to), itemsCache, true)
                }, { it ->
                    println(it)
                    it.filter { it is AggregatedDuration }.map { it as AggregatedDuration }.toList()
                }).subscribe({ it -> println("duration list received"); data.clear(); data.addAll(it); println(data); finished.invoke(true) }, { finished.invoke(false) })
        )
    }

    private fun timeToRatio(time: Long, pivot: Long): Float
    {
        return (time - pivot)/ DateUtils.HOUR_IN_MILLIS.toFloat()
    }

    override fun recycle() {
        data.clear()
        subscriptions.clear()
    }

    override fun getChartDrawer(): AChartDrawer {
        return DurationChartDrawer()
    }

    override fun getDataPointAt(position: Int): AggregatedDuration {
        return data[position]
    }

    override fun getDataPoints(): List<AggregatedDuration> {
        return data
    }

    override val numDataPoints: Int
        get() = data.size

    inner class DurationBar : ADataEncodedDrawer<AggregatedDuration> {

        var drawRange: Boolean = false
        var frontalBound: RectF = RectF()
        var backBound: RectF = RectF()
        private var rectBoundCache: RectF = RectF()

        var paint: Paint

        var pointPaint: Paint
        var pointUnderPaint: Paint? = null

        var roundMode = false

        constructor(){
            paint= Paint()
            pointPaint = Paint()
        }

        constructor(paint: Paint, pointPaint: Paint, pointUnderPaint: Paint? = null) {
            this.paint = paint
            this.pointPaint = pointPaint
            this.pointUnderPaint = pointUnderPaint
        }

        override fun onDraw(canvas: Canvas) {
            if(drawRange)
            {
                val originalColor = paint.color
                paint.color = Color.LTGRAY
                paint.alpha = 100

                if(roundMode) {
                    rectBoundCache.set(backBound.left, backBound.top - backBound.width() / 2, backBound.right, backBound.bottom + backBound.width() / 2)
                    canvas.drawRoundRect(rectBoundCache, 1000f, 1000f, paint)
                    //backBound.set(backBound.left, backBound.top + backBound.width() / 2, backBound.right, backBound.bottom - backBound.width() / 2)
                }
                else{
                    canvas.drawRoundRect(backBound, 2f, 2f, paint)
                }

                paint.color = originalColor
            }

            paint.alpha = 200

            if(roundMode) {
                rectBoundCache.set(frontalBound.left, frontalBound.top - frontalBound.width() / 2, frontalBound.right, frontalBound.bottom + frontalBound.width() / 2)
                canvas.drawRoundRect(rectBoundCache, 1000f, 1000f, paint)
                //frontalBound.set(frontalBound.left, frontalBound.top + frontalBound.width() / 2, frontalBound.right, frontalBound.bottom - frontalBound.width() / 2)


                pointPaint.alpha = 255

                if (pointUnderPaint != null) {
                    canvas.drawCircle(frontalBound.centerX(), frontalBound.top, frontalBound.width() / 2, pointUnderPaint)

                    canvas.drawCircle(frontalBound.centerX(), frontalBound.bottom, frontalBound.width() / 2, pointUnderPaint)
                }

                canvas.drawCircle(frontalBound.centerX(), frontalBound.top, frontalBound.width() / 2, pointPaint)

                canvas.drawCircle(frontalBound.centerX(), frontalBound.bottom, frontalBound.width() / 2, pointPaint)

                /*
                val originalColor = pointPaint.color
                pointPaint.color = Color.WHITE
                pointPaint.alpha = 200
                canvas.drawCircle(frontalBound.centerX(), frontalBound.top, frontalBound.width() / 2 - 2 * OTApplication.app.resourcesWrapped.displayMetrics.density, pointPaint)

                canvas.drawCircle(frontalBound.centerX(), frontalBound.bottom, frontalBound.width() / 2 - 2 * OTApplication.app.resourcesWrapped.displayMetrics.density, pointPaint)

                pointPaint.color = originalColor
*/
            }
            else{
                canvas.drawRoundRect(frontalBound, 2f, 2f, paint)
            }
        }

    }

    inner class DurationChartDrawer(): ATimelineChartDrawer(){

        override val aspectRatio: Float = 1.5f

        val verticalAxis = Axis(Axis.Pivot.LEFT)

        val yScale = NumericScale()

        private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var pointUnderPaint = Paint(Paint.ANTI_ALIAS_FLAG)


        val durationBars = DataEncodedDrawingList<AggregatedDuration, Void?>()

        val durationBarMaxWidth: Float by lazy { OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_duration_bar_max_width) }
        var durationBarWidth: Float = 0f

        init{

            paint.style = Paint.Style.FILL
            paint.color = ContextCompat.getColor(OTApplication.app, R.color.colorPointed_Light)

            pointPaint.strokeWidth = 2f * OTApplication.app.resourcesWrapped.displayMetrics.density
            pointPaint.style = Paint.Style.STROKE
            pointPaint.color = ContextCompat.getColor(OTApplication.app, R.color.colorPointed)

            pointUnderPaint.style = Paint.Style.FILL
            pointUnderPaint.color = Color.WHITE
            pointUnderPaint.alpha = 100

            paddingLeft = OTApplication.app.resourcesWrapped.getDimension(R.dimen.vis_axis_width_extended).toFloat()

            verticalAxis.style = Axis.TickLabelStyle.Small
            verticalAxis.drawBar = false
            verticalAxis.drawGridLines = true
            verticalAxis.scale = yScale

            horizontalAxis.drawGridLines = false

            yScale.setDomain(-3f, 15f, false).inverse().setTicksForEvery(3f)

            yScale.tickFormat = object: IAxisScale.ITickFormat<Float> {
                override fun format(value: Float, index: Int): String {

                    return when (value.toInt()) {
                        0 -> OTApplication.app.resourcesWrapped.getString(R.string.msg_midnight)
                        -12 -> OTApplication.app.resourcesWrapped.getString(R.string.msg_noon)
                        12 -> OTApplication.app.resourcesWrapped.getString(R.string.msg_noon)
                        else -> String.format("%02d", ((12 + value.toInt()) % 12)) + ":00"
                    }

                }
            }



            children.add(verticalAxis)
            children.add(durationBars)
        }

        override fun onModelChanged() {

        }

        override fun onRefresh() {
            super.onRefresh()

            durationBarWidth = Math.min(durationBarMaxWidth, xScale.getTickInterval() - 2.5f * OTApplication.app.resourcesWrapped.displayMetrics.density)

            durationBars.setData(this@DurationTimelineModel.data)

            durationBars.appendEnterSelection {
                datum->
                val newBar = DurationBar(paint, pointPaint, pointUnderPaint)
                    updateBarSize(newBar, datum.value)
                    newBar
            }

            durationBars.updateElement { datum, element ->
                val bar = element as DurationBar
                updateBarSize(bar, datum.value)
            }

            durationBars.removeElements(durationBars.getExitElements())


        }

        override fun onResized() {
            super.onResized()
            verticalAxis.attachedTo = plotAreaRect

            durationBars.onResizedCanvas { datum, element ->
                updateBarSize(element as DurationBar, datum.value)
            }
        }

        private fun updateBarSize(durationBar: DurationBar, datum: AggregatedDuration)
        {
            println("updating bar size: ${datum}")
            val centerX = xScale[datum.time]

            durationBar.roundMode = true

            durationBar.drawRange = datum.count>1
            if(durationBar.drawRange)
            {
                durationBar.backBound.set(centerX - durationBarWidth/2, yScale[datum.earliest], centerX + durationBarWidth/2, yScale[datum.latest])
            }

            durationBar.frontalBound.set(centerX - durationBarWidth/2, yScale[datum.avgFrom], centerX + durationBarWidth/2, yScale[datum.avgTo])
        }


        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
        }

    }

}