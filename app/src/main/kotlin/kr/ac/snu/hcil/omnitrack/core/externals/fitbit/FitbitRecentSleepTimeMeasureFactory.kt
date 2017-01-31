package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import okhttp3.HttpUrl
import org.json.JSONObject
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitRecentSleepTimeMeasureFactory : OTMeasureFactory("slp") {
    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_TIMESPAN
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
    override val isDemandingUserInput: Boolean = false


    override val exampleAttributeType: Int = OTAttribute.TYPE_TIMESPAN

    override fun makeMeasure(): OTMeasure {
        return FitbitRecentSleepTimeMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitRecentSleepTimeMeasure(serialized)
    }

    override val service: OTExternalService = FitbitService
    override val descResourceId: Int = R.string.measure_fitbit_sleep_time_desc
    override val nameResourceId: Int = R.string.measure_fitbit_sleep_time_name

    class FitbitRecentSleepTimeMeasure : OTRangeQueriedMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN
        override val factory: OTMeasureFactory = FitbitRecentSleepTimeMeasureFactory

        val converter = object : OAuth2Client.OAuth2RequestConverter<TimeSpan?> {
            override fun process(requestResultStrings: Array<String>): TimeSpan? {
                val json = JSONObject(requestResultStrings.first())

                if (json.has("sleep")) {
                    val sleeps = json.getJSONArray("sleep")

                    if (sleeps.length() > 0) {
                        val lastObj = sleeps.getJSONObject(0)
                        val dateOfSleepString = lastObj.getString("dateOfSleep") // yyyy-mm-dd
                        val duration = lastObj.getLong("duration") //millis
                        val minuteData = lastObj.getJSONArray("minuteData")
                        val startTimeString = minuteData.getJSONObject(0).getString("dateTime") // hh:mm:ss

                        val startTime = AuthConstants.DATE_TIME_FORMAT_WITHOUT_TIMEZONE.parse(dateOfSleepString + 'T' + startTimeString)
                        return TimeSpan.fromDuration(startTime.time, duration)
                    } else return null
                } else return null

            }

        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>> {
            val uri = HttpUrl.parse(FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SLEEP, Date(start)))
                    .newBuilder()
                    .addQueryParameter("isMainSleep", "true")
                    .build()
            return FitbitService.getRequest(
                    converter,
                    uri.toString()) as Observable<Result<out Any>>
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else if (other is FitbitRecentSleepTimeMeasure) {
                return true
            } else return false
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}