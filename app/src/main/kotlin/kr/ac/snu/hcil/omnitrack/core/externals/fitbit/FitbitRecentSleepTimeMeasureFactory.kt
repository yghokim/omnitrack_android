package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import com.google.gson.stream.JsonReader
import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import okhttp3.HttpUrl
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitRecentSleepTimeMeasureFactory : OTMeasureFactory("slp") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_TIMESPAN
    }

    override fun getAttributeType() = OTAttributeManager.TYPE_TIMESPAN

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
    override val isDemandingUserInput: Boolean = false


    override val exampleAttributeType: Int = OTAttributeManager.TYPE_TIMESPAN

    override fun makeMeasure(): OTMeasure {
        return FitbitRecentSleepTimeMeasure()
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return FitbitRecentSleepTimeMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitRecentSleepTimeMeasure()
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val descResourceId: Int = R.string.measure_fitbit_sleep_time_desc
    override val nameResourceId: Int = R.string.measure_fitbit_sleep_time_name

    override fun getService(): OTExternalService {
        return FitbitService
    }

    class FitbitRecentSleepTimeMeasure : OTRangeQueriedMeasure() {

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

        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {
            val uri = HttpUrl.parse(FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SLEEP, Date(start)))!!.newBuilder()
                    .addQueryParameter("isMainSleep", "true")
                    .build()
            return FitbitService.getRequest(
                    converter,
                    uri.toString()).toFlowable() as Flowable<Nullable<out Any>>
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is FitbitRecentSleepTimeMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}