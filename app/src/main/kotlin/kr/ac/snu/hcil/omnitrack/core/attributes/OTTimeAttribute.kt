package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute : OTAttribute<TimePoint> {

    companion object {
        const val GRANULARITY = 0

        const val GRANULARITY_SECOND = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_HOUR = 2
        const val GRANULARITY_DAY = 3
    }

    private val calendar = GregorianCalendar()

    constructor(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : super(objectId, dbId, columnName, OTAttribute.TYPE_TIME, settingData)

    constructor(columnName: String) : this(null, null, columnName, null)

    var granularity: Int
        get() = getPropertyValue<Int>(GRANULARITY)
        set(value) = setPropertyValue(GRANULARITY, value)

    override fun onPropertyValueChanged(args: OTProperty.PropertyChangedEventArgs<out Any>) {
        super.onPropertyValueChanged(args)
    }

    override val keys: Array<Int>
        get() = arrayOf(GRANULARITY)

    override fun createProperties() {
        assignProperty(OTSelectionProperty(GRANULARITY, "TimePoint Granularity", arrayOf("Second", "Minute", "Hour", "Day"))) //TODO: I18N
    }

    override fun parseAttributeValue(storedValue: String): TimePoint {
        return TimePoint(storedValue)
    }

    override fun formatAttributeValue(value: Any): String {
        if (value is TimePoint) {
            calendar.timeInMillis = value.timestamp
            calendar.timeZone = value.timezone

            calendar.set(Calendar.MILLISECOND, 0)

            when (granularity) {
                GRANULARITY_MINUTE -> calendar.set(Calendar.SECOND, 0)
                GRANULARITY_HOUR -> {
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MINUTE, 0)
                }

                GRANULARITY_DAY -> {
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.HOUR, 0)
                }
            }

            return calendar.toString()
        } else return ""
    }
}