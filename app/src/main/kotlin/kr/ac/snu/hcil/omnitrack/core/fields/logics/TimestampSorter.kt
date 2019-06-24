package kr.ac.snu.hcil.omnitrack.core.fields.logics

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
class TimestampSorter(val context: Context) : ItemComparator() {
    override val name: String get() = context.resources.getString(R.string.msg_sort_method_timestamp)

    override fun increasingCompare(a: OTItemDAO?, b: OTItemDAO?): Int {
        return (a?.timestamp ?: 0).compareTo(b?.timestamp ?: 0)
    }
}
