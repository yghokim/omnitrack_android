package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.components.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute : OTAttribute<TimePoint> {
    override val typeNameResourceId: Int = R.string.type_timepoint_name

    companion object {
        const val GRANULARITY = 0

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_TIME = 1
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
        assignProperty(OTSelectionProperty(GRANULARITY, "TimePoint Granularity", arrayOf("Day", "Time"))) //TODO: I18N
    }

    override fun parseAttributeValue(storedValue: String): TimePoint {
        return TimePoint(storedValue)
    }

    override fun formatAttributeValue(value: Any): String {
        if (value is TimePoint) {
            calendar.timeInMillis = value.timestamp
            calendar.timeZone = value.timezone

            calendar.set(Calendar.MILLISECOND, 0)

            if (granularity == GRANULARITY_DAY) {
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.HOUR, 0)
            }

            return calendar.toString()
        } else return ""
    }

    override fun makeDefaultValue(): TimePoint {
        return TimePoint()
    }

    override fun getInputView(context: Context, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == TYPE_TIME)) {
                    recycledView!! as TimePointInputView
                } else {
                    TimePointInputView(context)
                }

        when (granularity) {
            GRANULARITY_DAY -> view.setPickerMode(DateTimePicker.DATE)
            GRANULARITY_TIME -> view.setPickerMode(DateTimePicker.TIME)
        }

        return view
    }

}