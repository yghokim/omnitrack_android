package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
class TimestampSorter : ItemComparator() {
    override val name: String = OTApplication.app.resourcesWrapped.getString(R.string.msg_sort_method_timestamp)

    override fun increasingCompare(a: OTItemDAO?, b: OTItemDAO?): Int {
        return (a?.timestamp ?: 0).compareTo(b?.timestamp ?: 0)
    }
}
