package kr.ac.snu.hcil.omnitrack.core.visualization

/**
 * Created by younghokim on 2017. 11. 24..
 */
interface IWebBasedChartModel {
    fun getDataInJsonString(): String
    fun getChartTypeCommand(): String
}