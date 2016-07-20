package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute : OTAttribute<Long> {

    companion object {
        const val GRANULARITY = 0

        const val GRANULARITY_SECOND = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_HOUR = 2
        const val GRANULARITY_DAY = 3
    }

    constructor(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : super(objectId, dbId, columnName, OTAttribute.TYPE_TIME, settingData)

    constructor(columnName: String) : this(null, null, columnName, null)


    override val keys: Array<Int>
        get() = arrayOf(GRANULARITY)

    override fun createProperties() {
        assignProperty(OTSelectionProperty(GRANULARITY, "Time Granularity", arrayOf("Second", "Minute", "Hour", "Day"))) //TODO: I18N
    }
}