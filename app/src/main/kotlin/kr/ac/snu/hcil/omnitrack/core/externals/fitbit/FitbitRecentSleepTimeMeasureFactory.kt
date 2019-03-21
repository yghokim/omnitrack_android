package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.net.AuthConstants
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan
import okhttp3.HttpUrl
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
class FitbitRecentSleepTimeMeasureFactory(context: Context, service: FitbitService) : OTServiceMeasureFactory(context, service, "slp") {

    override fun getAttributeType() = OTAttributeManager.TYPE_TIMESPAN

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN
    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Hour
    override val isDemandingUserInput: Boolean = false


    override fun makeMeasure(): OTMeasure {
        return FitbitRecentSleepTimeMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return FitbitRecentSleepTimeMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitRecentSleepTimeMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val descResourceId: Int = R.string.measure_fitbit_sleep_time_desc
    override val nameResourceId: Int = R.string.measure_fitbit_sleep_time_name

    class FitbitRecentSleepTimeMeasure(factory: FitbitRecentSleepTimeMeasureFactory) : OTRangeQueriedMeasure(factory) {


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

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {
            val uri = HttpUrl.parse(FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SLEEP, Date(start)))!!.newBuilder()
                    .addQueryParameter("isMainSleep", "true")
                    .build()
            return getFactory<FitbitRecentSleepTimeMeasureFactory>().getService<FitbitService>().getRequest(
                    converter,
                    uri.toString()) as Single<Nullable<out Any>>
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