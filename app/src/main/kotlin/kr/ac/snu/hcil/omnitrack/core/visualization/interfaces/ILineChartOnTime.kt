package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
interface ILineChartOnTime : IChartInterface<ILineChartOnTime.TimeSeriesTrendData> {
    data class TimeSeriesTrendData(val points: Array<Pair<Long, BigDecimal>>) {
        fun maxValue(): BigDecimal {
            return points.maxWith(ValueComparator)?.second ?: BigDecimal.valueOf(0)
        }

        fun minValue(): BigDecimal {
            return points.minWith(ValueComparator)?.second ?: BigDecimal.valueOf(0)
        }

        companion object {
            val ValueComparator = Comparator<Pair<Long, BigDecimal>> {
                p0: Pair<Long, BigDecimal>, p1: Pair<Long, BigDecimal> ->
                p0.second.compareTo(p1.second)
            }

            val BigDecimalComparetor = Comparator<BigDecimal> {
                a: BigDecimal, b: BigDecimal ->
                a.compareTo(b)
            }

            fun maxValue(vararg lines: TimeSeriesTrendData): BigDecimal {
                return lines.map { it.maxValue() }.maxWith(BigDecimalComparetor) ?: BigDecimal.valueOf(0)
            }


            fun minValue(vararg lines: TimeSeriesTrendData): BigDecimal {
                return lines.map { it.minValue() }.minWith(BigDecimalComparetor) ?: BigDecimal.valueOf(0)
            }

        }
    }
}