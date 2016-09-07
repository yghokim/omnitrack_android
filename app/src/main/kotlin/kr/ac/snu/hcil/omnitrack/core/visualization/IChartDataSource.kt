package kr.ac.snu.hcil.omnitrack.core.visualization

/**
 * Created by younghokim on 16. 9. 7..
 */
interface IChartDataSource {
    fun generateRecommendedChartModels(): Array<ChartModel>
}