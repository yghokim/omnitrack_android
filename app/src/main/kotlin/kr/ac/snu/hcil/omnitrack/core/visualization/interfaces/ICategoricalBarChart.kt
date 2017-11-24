package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

import java.util.*

/**
 * Created by younghokim on 16. 9. 7..
 */
interface ICategoricalBarChart : IChartInterface<ICategoricalBarChart.Point> {
    data class Point(val label: String, val value: Double, val category: Int) {
        override fun toString(): String {
            return "$label : $value, in category: ${category}"
        }

        class ValueComparator : Comparator<Point> {
            override fun compare(p0: Point, p1: Point): Int {
                return p0.value.compareTo(p1.value)
            }

        }

        companion object {
            val VALUE_COMPARATOR = ValueComparator()
        }

        fun toJsonString(): String {
            return "{\"label\": \"$label\", \"value\":$value, \"category\":\"$category\"}"
        }
    }


}