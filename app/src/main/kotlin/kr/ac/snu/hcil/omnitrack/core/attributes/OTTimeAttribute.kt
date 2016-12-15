package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute(objectId: String?, dbId: Long?, columnName: String, isRequired: Boolean, settingData: String?, connectionData: String?) : OTAttribute<TimePoint>(objectId, dbId, columnName, isRequired, OTAttribute.TYPE_TIME, settingData, connectionData) {

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


    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, true)

    private val calendar = GregorianCalendar()

    var granularity: Int
        get() = getPropertyValue<Int>(GRANULARITY)
        set(value) = setPropertyValue(GRANULARITY, value)

    override fun onPropertyValueChanged(args: OTProperty.PropertyChangedEventArgs<out Any>) {
        super.onPropertyValueChanged(args)
    }

    override val propertyKeys: IntArray = intArrayOf(GRANULARITY)

    override fun createProperties() {
        assignProperty(OTSelectionProperty(GRANULARITY,
                OTApplication.app.resources.getString(R.string.property_time_granularity),
                arrayOf(OTApplication.app.resources.getString(R.string.property_time_granularity_day),
                        OTApplication.app.resources.getString(R.string.property_time_granularity_minute),
                        OTApplication.app.resources.getString(R.string.property_time_granularity_second)

                )))

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


    override fun getAutoCompleteValue(): Observable<TimePoint> {
        return Observable.just(TimePoint())
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

        if (inputView is TimePointInputView) {
            when (granularity) {
                GRANULARITY_DAY -> inputView.setPickerMode(DateTimePicker.DATE)
                GRANULARITY_MINUTE -> inputView.setPickerMode(DateTimePicker.MINUTE)
                GRANULARITY_SECOND -> inputView.setPickerMode(DateTimePicker.SECOND)
            }

            inputView.value = TimePoint()

            /*
            getAutoCompleteValueAsync { result ->
                inputView.value = result
            }*/
        }
    }

    override fun compareValues(a: Any, b: Any): Int {
        if (a is TimePoint && b is TimePoint) {
            return a.compareTo(b)
        } else return 0
    }
}