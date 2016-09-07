package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem

/**
 * Created by Young-Ho Kim on 2016-09-07.
 */
class TimestampSorter : ItemComparator() {
    override val name: String = OTApplication.app.resources.getString(R.string.msg_sort_method_timestamp)

    override fun increasingCompare(itemA: OTItem, itemB: OTItem): Int {
        return itemA.timestamp.compareTo(itemB.timestamp)
    }
}
