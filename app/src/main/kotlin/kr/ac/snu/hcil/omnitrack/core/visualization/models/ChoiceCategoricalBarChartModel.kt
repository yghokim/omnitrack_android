package kr.ac.snu.hcil.omnitrack.core.visualization.models

import android.util.SparseIntArray
import io.reactivex.Single
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.AttributeChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.IWebBasedChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.ICategoricalBarChart
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 16. 9. 7..
 */
class ChoiceCategoricalBarChartModel(attribute: OTAttributeDAO, realm: Realm, val configuredContext: ConfiguredContext) : AttributeChartModel<ICategoricalBarChart.Point>(attribute, realm), ICategoricalBarChart, IWebBasedChartModel {

    private val counterDictCache = SparseIntArray() // entry id : count
    private val categoriesCache = HashSet<Int>()

    override val name: String
        get() = String.format(configuredContext.applicationContext.resources.getString(R.string.msg_vis_categorical_distribution_title_format), super.name)

    @Inject
    protected lateinit var attributeManager: OTAttributeManager

    init{
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun recycle() {
        counterDictCache.clear()
        categoriesCache.clear()
    }

    override fun reloadData(): Single<List<ICategoricalBarChart.Point>> {
        val trackerId = attribute.trackerId
        println("reload data for tracker ${trackerId} - ChoiceCategorical")
        if (trackerId != null) {
            return dbManager
                    .makeItemsQuery(trackerId, getTimeScope(), realm)
                    .sort("timestamp", Sort.ASCENDING)
                    .findAllAsync()
                    .asFlowable()
                    .filter { it.isLoaded && it.isValid }
                    .firstOrError().map {
                items ->

                val data = ArrayList<ICategoricalBarChart.Point>()

                counterDictCache.clear()
                categoriesCache.clear()

                var noResponseCount = 0

                items.map { it.getValueOf(attribute.localId) as? IntArray }
                        .forEach {
                            if (it != null && it.isNotEmpty()) {
                                for (id in it) {
                                    if (!categoriesCache.contains(id)) {
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

                synchronized(data) {
                    data.clear()
                    for (categoryId in categoriesCache) {
                        val entry = (attributeManager.getAttributeHelper(OTAttributeManager.TYPE_CHOICE) as OTChoiceAttributeHelper).getChoiceEntries(attribute)?.findWithId(categoryId)
                        println("entry: ${entry?.text}, count: ${counterDictCache[categoryId]}")
                        if (entry != null) {
                            println("entry add")
                            data.add(ICategoricalBarChart.Point(entry.text, counterDictCache[categoryId].toDouble(), categoryId))
                        }
                    }
                }

                println("result data: " + data)

                categoriesCache.clear()
                counterDictCache.clear()

                return@map data.toList()
            }
        } else {
            throw IllegalArgumentException("No tracker is assigned in the field.")
        }
    }

    override fun getDataInJsonString(): String {
        return "{\"data\":[${cachedData.joinToString(", ") { it.toJsonString() }}]}"
    }

    override fun getChartTypeCommand(): String {
        return "horizontal-bar"
    }


    /*
    override fun getChartDrawer(): AChartDrawer {
        val drawer = CategoricalBarChartDrawer()
        drawer.integerValues = true

        return drawer
    }*/

}