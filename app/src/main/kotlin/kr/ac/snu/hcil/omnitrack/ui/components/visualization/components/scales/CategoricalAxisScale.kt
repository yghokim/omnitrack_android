package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales

import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.IAxisScale
import java.util.*

/**
 * Created by Young-Ho on 9/8/2016.
 */
class CategoricalAxisScale: IAxisScale<Int> {
    override var tickFormat: IAxisScale.ITickFormat<Int>? = null

    private var rangeFrom: Float = 0f
    private var rangeTo: Float = 0f

    private val categoryList = ArrayList<String>()

    private var tickInterval: Float = 0f

    override fun setRealCoordRange(from: Float, to: Float): CategoricalAxisScale {
        this.rangeFrom = from
        this.rangeTo = to

        tickInterval = if(numTicks>0)
            ((to - from)/numTicks)
            else 0f

        return this
    }

    fun setCategories(vararg categories: String): CategoricalAxisScale {
        categoryList.clear()
        categoryList.addAll(categories)

        return this
    }

    override val numTicks: Int get()= categoryList.size

    override fun getTickCoordAt(index: Int): Float {
        return rangeFrom + tickInterval * index + tickInterval/2
    }

    override fun getTickInterval(): Float {
        return tickInterval
    }


    override fun getTickLabelAt(index: Int): String {
        return categoryList[index]
    }


    override fun get(domain: Int): Float {
        return getTickCoordAt(domain)
    }

}