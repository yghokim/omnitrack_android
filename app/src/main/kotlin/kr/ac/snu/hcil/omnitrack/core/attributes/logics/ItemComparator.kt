package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-07.
 */
abstract class ItemComparator : Comparator<OTItemDAO> {
    abstract val name: String

    var isDecreasing: Boolean = true

    override fun toString(): String {
        return name
    }

    final override fun compare(p0: OTItemDAO?, p1: OTItemDAO?): Int {
        if (isDecreasing) {
            return -1 * increasingCompare(p0, p1)
        } else {
            return increasingCompare(p0, p1)
        }
    }

    abstract fun increasingCompare(a: OTItemDAO?, b: OTItemDAO?): Int

}

