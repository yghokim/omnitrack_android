package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.android.common.time.TimeHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
class FitbitDistanceMeasureFactory(context: Context, parentService: FitbitService) : OTServiceMeasureFactory(context, parentService, "dist") {

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_FLOAT

    override fun makeMeasure(): OTMeasure {
        return FitbitDistanceMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return FitbitDistanceMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitDistanceMeasure(this)
    }


    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val descResourceId: Int = R.string.measure_fitbit_distance_desc
    override val nameResourceId: Int = R.string.measure_fitbit_distance_name


    class FitbitDistanceMeasure(factory: FitbitDistanceMeasureFactory) : OTRangeQueriedMeasure(factory) {

        val dailyConverter = object : OAuth2Client.OAuth2RequestConverter<Float?> {
            override fun process(requestResultStrings: Array<String>): Float? {
                val json = JSONObject(requestResultStrings.first())
                println("convert $json")
                if (json.has("summary")) {
                    return Math.round(json.getJSONObject("summary").getJSONArray("distances").getJSONObject(0).getDouble("distance") * 100) / 100f

                } else return null
            }
        }

        val intraDayConverter = object : FitbitApi.AIntraDayConverter<Float, Float>("activities-distance-intraday") {
            override fun extractValueFromDatum(datum: JSONObject): Float {
                return datum.getDouble("value").toFloat()
            }

            override fun processValues(values: List<Float>): Float {
                return (Math.round(values.sum() * 100) / 100f)
            }

        }

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {

            return if (TimeHelper.isSameDay(start, end - 10)) {
                getFactory<FitbitDistanceMeasureFactory>().getService<FitbitService>().getRequest(
                        dailyConverter,
                        FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SUMMARY, Date(start)))
                        as Single<Nullable<out Any>>
            } else
            //TODO: Can be optimized by querying summary data of middle days.
                getFactory<FitbitDistanceMeasureFactory>().getService<FitbitService>().getRequest(intraDayConverter, *FitbitApi.makeIntraDayRequestUrls(FitbitApi.REQUEST_INTRADAY_RESOURCE_PATH_DISTANCE, start, end))
                        as Single<Nullable<out Any>>
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is FitbitDistanceMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}