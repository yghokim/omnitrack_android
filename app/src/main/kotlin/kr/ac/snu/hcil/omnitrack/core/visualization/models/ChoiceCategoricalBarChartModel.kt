package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.util.SparseIntArray
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.attributes.OTChoiceAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import kr.ac.snu.hcil.omnitrack.core.visualization.AttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.CategoricalBarChartDrawer
import java.util.*

/**
 * Created by younghokim on 16. 9. 7..
 */
class ChoiceCategoricalBarChartModel(override val attribute: OTChoiceAttribute) : AttributeChartModel<ICategoricalBarChart.Point>(attribute), ICategoricalBarChart {
    private var loaded = false
    private val data = ArrayList<ICategoricalBarChart.Point>()

    private val itemsCache = ArrayList<OTItem>()
    private val counterDictCache = SparseIntArray() // entry id : count
    private val categoriesCache = HashSet<Int>()

    override val isLoaded: Boolean get() = !loaded

    override val numDataPoints: Int get() = data.size

    override val name: String
        get() = String.format(OTApplication.app.resources.getString(R.string.msg_vis_categorical_distribution_title_format), super.name)

    override fun recycle() {
        data.clear()
    }

    override fun onReload(finished: (Boolean) -> Unit) {

        data.clear()

        val tracker = attribute.tracker
        if (tracker != null) {
            itemsCache.clear()
            FirebaseHelper.loadItems(tracker, getTimeScope()).subscribe({
                items ->
                itemsCache.addAll(items)
                counterDictCache.clear()
                categoriesCache.clear()

                var noResponseCount = 0

                itemsCache
                        .map { it.getValueOf(attribute) as? IntArray }
                        .forEach {
                            if (it != null && it.size > 0) {
                                for (id in it) {
                                    if (categoriesCache.contains(id) == false) {
                                        categoriesCache.add(id)
                                        counterDictCache.put(id, 1)
                                    } else {
                                        counterDictCache.put(id, counterDictCache[id] + 1)
                                    }
                                }
                            } else {
                                noResponseCount++
                            }
                        }
                itemsCache.clear()

                for (categoryId in categoriesCache)
                {
                    val entry = attribute.entries.findWithId(categoryId)
                    println("entry: ${entry?.text}, count: ${counterDictCache[categoryId]}")
                    if (entry != null) {
                        println("entry add")
                        data.add(ICategoricalBarChart.Point(entry.text, counterDictCache[categoryId].toDouble(), categoryId))
                    }
                }

                println("result data: " + data)

                categoriesCache.clear()
                counterDictCache.clear()
                finished.invoke(true)
            }, {
                finished.invoke(false)
            })
        }
    }


    override fun getDataPoints(): List<ICategoricalBarChart.Point> {
        return data
    }


    override fun getDataPointAt(position: Int): ICategoricalBarChart.Point {
        return data[position]
    }


    override fun getChartDrawer(): AChartDrawer {
        val drawer = CategoricalBarChartDrawer()
        drawer.integerValues = true

        return drawer
    }

}