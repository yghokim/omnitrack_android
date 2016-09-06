package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute : OTAttribute<TimePoint> {
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT


    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_POINT
    }

    override val typeNameResourceId: Int = R.string.type_timepoint_name
    override val typeSmallIconResourceId: Int = R.drawable.icon_small_time

    companion object {
        const val GRANULARITY = 0

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_SECOND = 2


        val formats = mapOf<Int, SimpleDateFormat>(
                Pair(GRANULARITY_DAY, SimpleDateFormat("yyyy/MM/d")),
                Pair(GRANULARITY_MINUTE, SimpleDateFormat("yyyy/MM/d h:mm a")),
                Pair(GRANULARITY_SECOND, SimpleDateFormat("yyyy/MM/d h:mm:ss a"))
        )
    }

    private val calendar = GregorianCalendar()

    constructor(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?) : super(objectId, dbId, columnName, OTAttribute.TYPE_TIME, settingData, connectionData)

    var granularity: Int
        get() = getPropertyValue<Int>(GRANULARITY)
        set(value) = setPropertyValue(GRANULARITY, value)

    override fun onPropertyValueChanged(args: OTProperty.PropertyChangedEventArgs<out Any>) {
        super.onPropertyValueChanged(args)
    }

    override val propertyKeys: IntArray = intArrayOf(GRANULARITY)

    override fun createProperties() {
        assignProperty(OTSelectionProperty(GRANULARITY, "TimePoint Granularity", arrayOf("Day", "Minute", "Second"))) //TODO: I18N
        setPropertyValue(GRANULARITY, GRANULARITY_MINUTE)
    }

    override fun formatAttributeValue(value: Any): String {
        if (value is TimePoint) {
            calendar.timeInMillis = value.timestamp
            calendar.timeZone = value.timeZone

            calendar.set(Calendar.MILLISECOND, 0)

            if (granularity == GRANULARITY_DAY) {
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }

            return formats[granularity]!!.format(calendar.time)
        } else return ""
    }

    override fun getAutoCompleteValueAsync(resultHandler: (TimePoint) -> Unit): Boolean {
        resultHandler(TimePoint())
        return true
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

        if (inputView is TimePointInputView) {
            when (granularity) {
                GRANULARITY_DAY -> inputView.setPickerMode(DateTimePicker.DATE)
                GRANULARITY_MINUTE -> inputView.setPickerMode(DateTimePicker.MINUTE)
                GRANULARITY_SECOND -> inputView.setPickerMode(DateTimePicker.SECOND)
            }

            /*
            getAutoCompleteValueAsync { result ->
                inputView.value = result
            }*/
        }
    }


}