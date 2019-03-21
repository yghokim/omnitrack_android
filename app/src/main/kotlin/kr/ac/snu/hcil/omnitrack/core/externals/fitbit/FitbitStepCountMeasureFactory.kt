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
class FitbitStepCountMeasureFactory(context: Context, parentService: FitbitService) : OTServiceMeasureFactory(context, parentService, "step") {

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Hour
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return FitbitStepMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return FitbitStepMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitStepMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val descResourceId: Int = R.string.measure_steps_desc
    override val nameResourceId: Int = R.string.measure_steps_name


    class FitbitStepMeasure(factory: FitbitStepCountMeasureFactory) : OTRangeQueriedMeasure(factory) {

        val dailyConverter = object : OAuth2Client.OAuth2RequestConverter<Int?> {
            override fun process(requestResultStrings: Array<String>): Int? {
                val json = JSONObject(requestResultStrings.first())
                println("convert $json")
                if (json.has("summary")) {
                    return json.getJSONObject("summary").getInt("steps")

                } else return null
            }

        }

        val intraDayConverter = object : FitbitApi.AIntraDayConverter<Int, Int>("activities-steps-intraday") {
            override fun extractValueFromDatum(datum: JSONObject): Int {
                return datum.getInt("value")
            }

            override fun processValues(values: List<Int>): Int {
                return values.sum()
            }

        }

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {

            return if (TimeHelper.isSameDay(start, end - 10)) {
                getFactory<FitbitStepCountMeasureFactory>().getService<FitbitService>().getRequest(
                        dailyConverter,
                        FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SUMMARY, Date(start)))
                        as Single<Nullable<out Any>>
            } else
            //TODO: Can be optimized by querying summary data of middle days.
                getFactory<FitbitStepCountMeasureFactory>().getService<FitbitService>().getRequest(intraDayConverter, *FitbitApi.makeIntraDayRequestUrls(FitbitApi.REQUEST_INTRADAY_RESOURCE_PATH_STEPS, start, end))
                        as Single<Nullable<out Any>>
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is FitbitStepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}