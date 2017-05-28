package kr.ac.snu.hcil.omnitrack.core.attributes

import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTSelectionProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.TimePointInputView
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class OTTimeAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?) : OTAttribute<TimePoint>(objectId, localKey, parentTracker, columnName, isRequired, OTAttribute.TYPE_TIME, settingData, connectionData) {

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT


    override val propertyKeys: Array<String> = arrayOf(GRANULARITY)

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_TIME_POINT
    }

    override val typeNameResourceId: Int = R.string.type_timepoint_name
    override val typeSmallIconResourceId: Int = R.drawable.icon_small_time

    companion object {
        const val GRANULARITY = "granularity"

        const val GRANULARITY_DAY = 0
        const val GRANULARITY_MINUTE = 1
        const val GRANULARITY_SECOND = 2


        val formats = mapOf<Int, SimpleDateFormat>(
                Pair(GRANULARITY_DAY, SimpleDateFormat(OTApplication.getString(R.string.property_time_format_granularity_day))),
                Pair(GRANULARITY_MINUTE, SimpleDateFormat(OTApplication.getString(R.string.property_time_format_granularity_minute))),
                Pair(GRANULARITY_SECOND, SimpleDateFormat(OTApplication.getString(R.string.property_time_format_granularity_second)))
        )

        private val timezoneSizeSpan = AbsoluteSizeSpan(OTApplication.app.resources.getDimensionPixelSize(R.dimen.tracker_list_element_information_text_headerSize))
        private val timezoneStyleSpan = StyleSpan(Typeface.BOLD)
        private val timezoneColorSpan = ForegroundColorSpan(ContextCompat.getColor(OTApplication.app.contextCompat, R.color.textColorLight))
    }


    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(true, true)

    private val calendar = GregorianCalendar()

    override val isAutoCompleteValueStatic: Boolean = false

    var granularity: Int
        get() = getPropertyValue<Int>(GRANULARITY)
        set(value) {
            setPropertyValue(GRANULARITY, value)
        }

    override fun createProperties() {
        assignProperty(OTSelectionProperty(GRANULARITY,
                OTApplication.app.resources.getString(R.string.property_time_granularity),
                arrayOf(OTApplication.app.resources.getString(R.string.property_time_granularity_day),
                        OTApplication.app.resources.getString(R.string.property_time_granularity_minute),
                        OTApplication.app.resources.getString(R.string.property_time_granularity_second)

                )))

        setPropertyValue(GRANULARITY, GRANULARITY_MINUTE)
    }

    override fun formatAttributeValue(value: Any): CharSequence {
        if (value is TimePoint) {
            calendar.timeInMillis = value.timestamp
            calendar.timeZone = value.timeZone

            calendar.set(Calendar.MILLISECOND, 0)

            if (granularity == GRANULARITY_DAY) {
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }

            val timeString = formats[granularity]!!.format(calendar.time)
            val timeZoneName = value.timeZone.displayName
            val start = timeString.length + 1
            val end = timeString.length + 1 + timeZoneName.length

            return SpannableString("$timeString\n$timeZoneName").apply {
                setSpan(timezoneSizeSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(timezoneStyleSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(timezoneColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
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

    override fun onAddColumnToTable(out: MutableList<String>) {
        out.add("${getAttributeUniqueName()}_timestamp_epoch")
        out.add("${getAttributeUniqueName()}_timezone")
        out.add("${getAttributeUniqueName()}_formatted")

    }

    override fun onAddValueToTable(value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if(value is TimePoint)
        {
            val date = Date(value.timestamp)
            TimeHelper.FORMAT_ISO_8601.timeZone = value.timeZone

            out.add(value.timestamp.toString())
            out.add(value.timeZone.getDisplayName(Locale.ENGLISH))
            out.add(TimeHelper.FORMAT_ISO_8601.format(date))
        }
        else{
            out.add(null)
            out.add(null)
        }
    }
}