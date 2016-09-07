package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

/**
 * Created by younghokim on 16. 9. 7..
 */
interface ICategoricalBarChart : IChartInterface<ICategoricalBarChart.Point> {
    data class Point(val label: String, val value: Double, val category: Int){
        override fun toString(): String {
            return "$label : $value, in category: ${category}"
        }
    }



}