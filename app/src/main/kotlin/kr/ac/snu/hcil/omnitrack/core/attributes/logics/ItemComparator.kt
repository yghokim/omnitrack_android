package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.OTItem
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-07.
 */
abstract class ItemComparator : Comparator<OTItem> {
    abstract val name: String

    var isDecreasing: Boolean = true

    companion object {
        val TIMESTAMP_SORTER = TimestampSorter()
    }

    override fun toString(): String {
        return name
    }

    override final fun compare(p0: OTItem, p1: OTItem): Int {
        if (isDecreasing) {
            return -1 * increasingCompare(p0, p1)
        } else {
            return increasingCompare(p0, p1)
        }
    }

    abstract fun increasingCompare(a: OTItem, b: OTItem): Int


}

