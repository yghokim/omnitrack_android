package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.DurationTimelineModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.TimeRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimeRangePickerInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 16. 8. 6..
 */
class OTTimeSpanAttribute(objectId: String?, dbId: Long?, columnName: String, isRequired: Boolean, settingData: String?, connectionData: String?) : OTAttribute<TimeSpan>(objectId, dbId, columnName, isRequired, TYPE_TIMESPAN, settingData, connectionData) {

    companion object {
        const val PROPERTY_GRANULARITY = 0
        const val PROPERTY_TYPE = 1

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
    }

    override val propertyKeys: IntArray = intArrayOf(PROPERTY_GRANULARITY/*, PROPERTY_TYPE*/)

    override val typeNameResourceId: Int = R.string.type_timespan_name
    override val typeSmallIconResourceId: Int = R.drawable.icon_small_timer
    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

    override fun createProperties() {
        assignProperty(OTSelectionProperty(PROPERTY_GRANULARITY,
                OTApplication.app.resources.getString(R.string.property_time_granularity),
                arrayOf(OTApplication.app.resources.getString(R.string.property_time_granularity_day),
                        OTApplication.app.resources.getString(R.string.property_time_granularity_minute)
                )))
        //assignProperty(OTSelectionProperty(PROPERTY_TYPE, "Interface Type", arrayOf("Range Picker", "Stopwatch")))

        setPropertyValue(PROPERTY_GRANULARITY, GRANULARITY_DAY)
    }

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, false)

    val granularity: Int get() = getPropertyValue(PROPERTY_GRANULARITY)

    override fun formatAttributeValue(value: Any): CharSequence {

        return (value as? TimeSpan)?.let {
            val format = when (granularity) {
                GRANULARITY_DAY -> OTTimeAttribute.formats[OTTimeAttribute.GRANULARITY_DAY]!!
                GRANULARITY_MINUTE -> OTTimeAttribute.formats[OTTimeAttribute.GRANULARITY_MINUTE]!!
                else -> OTTimeAttribute.formats[OTTimeAttribute.GRANULARITY_MINUTE]!!
            }

            val from = format.format(Date(it.from))
            val to = format.format(Date(it.to))

            val overlapUntil = 0
            /*
            while( from[overlapUntil] == to[overlapUntil] )
            {
                overlapUntil++
                if(overlapUntil>=from.length || overlapUntil >= to.length){break;}
            }*/

            val builder = StringBuilder(from)
            builder.append(" ~ ").append(to.subSequence(overlapUntil, to.length)).toString()

        } ?: ""
    }

    override fun getAutoCompleteValue(): Observable<TimeSpan> {
        return Observable.just(TimeSpan())
    }


    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_RANGE_PICKER
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if(inputView is TimeRangePickerInputView)
        {
            val granularity = when(getPropertyValue<Int>(PROPERTY_GRANULARITY))
            {
                0 -> TimeRangePicker.Granularity.DATE
                1 -> TimeRangePicker.Granularity.TIME
                else -> TimeRangePicker.Granularity.TIME
            }

            inputView.setGranularity(granularity)
        }
    }

    override fun getRecommendedChartModels(): Array<ChartModel<*>> {
        return arrayOf(DurationTimelineModel(this))
    }


}