package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
class TimestampSorter : ItemComparator() {
    override val name: String = OTApp.instance.resourcesWrapped.getString(R.string.msg_sort_method_timestamp)

    override fun increasingCompare(a: OTItemDAO?, b: OTItemDAO?): Int {
        return (a?.timestamp ?: 0).compareTo(b?.timestamp ?: 0)
    }
}
