package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import kr.ac.snu.hcil.omnitrack.utils.*
import java.util.*

/**
 * Created by Young-Ho on 9/9/2016.
 */

class QuantizedTimeScale : IAxisScale<Long> {

    companion object{


        private val calendarCache: Calendar by lazy{
            Calendar.getInstance()
        }

        val TICKFORMAT_DAY =  object: IAxisScale.ITickFormat<Long> {
            override fun format(time: Long, index: Int): String {
                calendarCache.timeInMillis = time
                val hourOfDay = calendarCache.getHourOfDay()

                return if (hourOfDay == 12) {
                    OTApplication.app.resources.getString(R.string.msg_noon)
                } else if (hourOfDay == 0 || hourOfDay == 23) {
                    TimeHelper.FORMAT_DAY.format(Date(time))
                } else {
                    "haha"
                }
            }
        }

        val TICKFORMAT_WEEK= object: IAxisScale.ITickFormat<Long> {
            override fun format(value: Long, index: Int): String {
                return TimeHelper.FORMAT_DAY_OF_WEEK_SHORT.format(Date(value))
            }

        }

        val TICKFORMAT_WEEK_2= object: IAxisScale.ITickFormat<Long> {
            override fun format(value: Long, index: Int): String {
                calendarCache.timeInMillis = value
                val dow = calendarCache.getDayOfWeek()
                if(dow == 1 && index != 0)
                {
                    return TimeHelper.FORMAT_DAY_WITHOUT_YEAR.format(Date(value))
                }
                else{
                    if(dow  == 1 || dow == 3 || dow == 5 || (dow==7 && index == 13)) {
                        return TimeHelper.FORMAT_DAY_OF_WEEK_SHORT.format(Date(value))
                    }
                    else return ""
                }
            }
        }

        val TICKFORMAT_MONTH = object: IAxisScale.ITickFormat<Long> {
            override fun format(value: Long, index: Int): String {
                calendarCache.timeInMillis = value
                val result = calendarCache.getDayOfMonth().toString()

                if(index == 0 || result.endsWith('5') || result.endsWith('0'))
                    return result
                else return ""
            }
        }

        val TICKFORMAT_YEAR = object: IAxisScale.ITickFormat<Long> {
            override fun format(value: Long, index: Int): String {
                return TimeHelper.FORMAT_MONTH_SHORT.format(Date(value))

            }
        }

    }


    override var tickFormat: IAxisScale.ITickFormat<Long>? = null

    private var rangeMin: Float = 0f

    private var rangeMax: Float = 0f

    private val domainBinPoints = ArrayList<Long>()

    //Calendar flag
    private var calendarLevel: Int = Calendar.DAY_OF_YEAR

    private var domainTimeMin: Long = 0
    private var domainTimeMax: Long = 0

    /**
     * inset start and end coord with half of bin size.
     */
    private var inset: Boolean = false

    private val calendarCache: Calendar by lazy{
        Calendar.getInstance()
    }


    fun inset(inset: Boolean): QuantizedTimeScale {
        this.inset = inset
        return this
    }

    override fun setRealCoordRange(from: Float, to: Float): QuantizedTimeScale {
        rangeMin = from
        rangeMax = to

        return this
    }

    override val numTicks: Int
        get() = domainBinPoints.size



    fun setDomain(from: Long, to: Long): QuantizedTimeScale
    {
        domainTimeMin = from
        domainTimeMax = to

        domainBinPoints.clear()
        domainBinPoints.add(domainTimeMin)
        domainBinPoints.add(domainTimeMax)


        return this
    }

    fun quantize(level: Int, every: Int = 1)
    {
        this.calendarLevel = level

        domainBinPoints.clear()
        calendarCache.timeInMillis = domainTimeMin
        var current: Long = domainTimeMin
        while(current < domainTimeMax)
        {
            domainBinPoints.add(current)
            calendarCache.add(calendarLevel, every)
            current = calendarCache.timeInMillis
        }

        println("binPoints: ${domainBinPoints}")
    }

    fun quantize(granularity: Granularity)
    {
        when(granularity)
        {
            Granularity.DAY ->
            {
                quantize(Calendar.HOUR_OF_DAY, 1)
                tickFormat = TICKFORMAT_DAY
            }

            Granularity.WEEK ->
            {
                quantize(Calendar.DAY_OF_YEAR, 1)
                tickFormat = TICKFORMAT_WEEK
            }

            Granularity.WEEK_2->
            {
                quantize(Calendar.DAY_OF_YEAR, 1)
                tickFormat = TICKFORMAT_WEEK_2
            }

            Granularity.MONTH ->
            {
                quantize(Calendar.DAY_OF_YEAR, 1)
                tickFormat = TICKFORMAT_MONTH
            }

            Granularity.YEAR ->{
                quantize(Calendar.MONTH, 1)
                tickFormat = TICKFORMAT_YEAR
            }
        }
    }

    override fun getTickLabelAt(index: Int): String {
        return tickFormat?.format(domainBinPoints[index], index) ?: domainBinPoints[index].toString()
    }

    override fun getTickInterval(): Float {
        val rangeWidth = rangeMax - rangeMin

        if(inset)
            return rangeWidth/(domainBinPoints.size-1 +1)
        else return rangeWidth/(domainBinPoints.size-1)
    }

    override fun getTickCoordAt(index: Int): Float {
        return this[domainBinPoints[index]]
    }

    override fun get(domain: Long): Float {
        val index = domainBinPoints.indexOf(domain)
        if(index!=-1)
        {
            val coord = rangeMin + getTickInterval() * index

            if(inset) return getTickInterval()/2 + coord
            else return coord
        }
        else return 0f //TODO quantize
    }


}