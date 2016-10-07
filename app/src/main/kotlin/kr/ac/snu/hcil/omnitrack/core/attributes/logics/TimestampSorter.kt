package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
class TimestampSorter : ItemComparator() {
    override val name: String = OTApplication.app.resources.getString(R.string.msg_sort_method_timestamp)

    override fun increasingCompare(a: OTItem, b: OTItem): Int {
        return a.timestamp.compareTo(b.timestamp)
    }
}
