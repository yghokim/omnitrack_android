package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import okhttp3.HttpUrl
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitRecentSleepTimeMeasureFactory : OTMeasureFactory() {
    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_TIMESPAN
    }

    override val isRangedQueryAvailable: Boolean = true

    override fun makeMeasure(): OTMeasure {
        return FitbitRecentSleepTimeMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitRecentSleepTimeMeasure(serialized)
    }

    override val service: OTExternalService = FitbitService
    override val descResourceId: Int = R.string.measure_fitbit_sleep_time_desc
    override val nameResourceId: Int = R.string.measure_fitbit_sleep_time_name

    class FitbitRecentSleepTimeMeasure : OTMeasure {


        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN
        override val factory: OTMeasureFactory = FitbitRecentSleepTimeMeasureFactory

        val converter = object : OAuth2Client.OAuth2RequestConverter<TimeSpan?> {
            override fun process(requestResultString: String): TimeSpan? {
                val json = JSONObject(requestResultString)

                if (json.has("sleep")) {
                    val sleeps = json.getJSONArray("sleep")

                    if (sleeps.length() > 0) {
                        val lastObj = sleeps.getJSONObject(0)
                        val dateOfSleepString = lastObj.getString("dateOfSleep") // yyyy-mm-dd
                        val duration = lastObj.getInt("duration") //millis
                        val minuteData = lastObj.getJSONArray("minuteData")
                        val startTimeString = minuteData.getJSONObject(0).getString("dateTime") // hh:mm:ss

                        val startTime = AuthConstants.DATE_TIME_FORMAT_WITHOUT_TIMEZONE.parse(dateOfSleepString + 'T' + startTimeString)
                        return TimeSpan(startTime.time, duration)
                    } else return null
                } else return null

            }

        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit) {
            val range = query!!.getRange(builder)
            val uri = HttpUrl.parse(FitbitService.makeRequestUrlWithCommandAndDate(FitbitService.REQUEST_COMMAND_SLEEP, Date(range.first)))
                    .newBuilder()
                    .addQueryParameter("isMainSleep", "true")
                    .build()
            FitbitService.request(
                    uri.toString(),
                    converter)
            {
                result ->
                handler.invoke(result)
            }
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }


    }
}