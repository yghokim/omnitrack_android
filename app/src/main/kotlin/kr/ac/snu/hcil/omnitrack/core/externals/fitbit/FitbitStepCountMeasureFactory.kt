package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
class FitbitStepCountMeasureFactory(context: Context, parentService: FitbitService) : OTMeasureFactory(context, parentService, "step") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
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

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

        val dailyConverter = object : OAuth2Client.OAuth2RequestConverter<Int?> {
            override fun process(requestResultStrings: Array<String>): Int? {
                val json = JSONObject(requestResultStrings.first())
                println("convert $json")
                if (json.has("summary")) {
                    val steps = json.getJSONObject("summary").getInt("steps")
                    return steps

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

        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {

            return if (TimeHelper.isSameDay(start, end - 10)) {
                service<FitbitService>().getRequest(
                        dailyConverter,
                        FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SUMMARY, Date(start))).toFlowable()
                        as Flowable<Nullable<out Any>>
            } else
            //TODO: Can be optimized by querying summary data of middle days.
                service<FitbitService>().getRequest(intraDayConverter, *FitbitApi.makeIntraDayRequestUrls(FitbitApi.REQUEST_INTRADAY_RESOURCE_PATH_STEPS, start, end)).toFlowable()
                        as Flowable<Nullable<out Any>>
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