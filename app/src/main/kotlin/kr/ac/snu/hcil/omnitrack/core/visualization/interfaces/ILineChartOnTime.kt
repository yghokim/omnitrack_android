package kr.ac.snu.hcil.omnitrack.core.visualization.interfaces

import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.visualization.ITimelineChart
import java.math.BigDecimal
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-08.
 */
interface ILineChartOnTime : IChartInterface<ILineChartOnTime.LineData>, ITimelineChart {
    data class LineData(val points: Array<Pair<Long, BigDecimal>>, val attribute: OTAttribute<out Any>) {
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

            fun maxValue(vararg lines: LineData): BigDecimal {
                return lines.map { it.maxValue() }.maxWith(BigDecimalComparetor) ?: BigDecimal.valueOf(0)
            }


            fun minValue(vararg lines: LineData): BigDecimal {
                return lines.map { it.minValue() }.minWith(BigDecimalComparetor) ?: BigDecimal.valueOf(0)
            }

        }
    }
}