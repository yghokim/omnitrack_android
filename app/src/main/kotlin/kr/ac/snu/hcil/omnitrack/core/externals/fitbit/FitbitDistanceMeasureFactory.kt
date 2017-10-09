package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.json.JSONObject
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitDistanceMeasureFactory : OTMeasureFactory("dist") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_DISTANCE_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return FitbitDistanceMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitDistanceMeasure(serialized)
    }

    override fun getService(): OTExternalService {
        return FitbitService
    }

    override val descResourceId: Int = R.string.measure_fitbit_distance_desc
    override val nameResourceId: Int = R.string.measure_fitbit_distance_name


    class FitbitDistanceMeasure : OTRangeQueriedMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_FLOAT

        override val factory: OTMeasureFactory = FitbitDistanceMeasureFactory

        val dailyConverter = object : OAuth2Client.OAuth2RequestConverter<Float?> {
            override fun process(requestResultStrings: Array<String>): Float? {
                val json = JSONObject(requestResultStrings.first())
                println("convert $json")
                if (json.has("summary")) {
                    val distance = Math.round(json.getJSONObject("summary").getJSONArray("distances").getJSONObject(0).getDouble("distance") * 100) / 100f
                    return distance

                } else return null
            }

        }

        val intraDayConverter = object : FitbitApi.AIntraDayConverter<Float, Float>("activities-distance-intraday") {
            override fun extractValueFromDatum(datum: JSONObject): Float {
                return datum.getDouble("value").toFloat()
            }

            override fun processValues(values: List<Float>): Float {
                return (Math.round(values.sum() * 100) / 100f).toFloat()
            }

        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)


        override fun getValueRequest(start: Long, end: Long): Observable<Nullable<out Any>> {

            return if (TimeHelper.isSameDay(start, end - 10)) {
                FitbitService.getRequest(
                        dailyConverter,
                        FitbitApi.makeDailyRequestUrl(FitbitApi.REQUEST_COMMAND_SUMMARY, Date(start)))
                        as Observable<Nullable<out Any>>
            } else
            //TODO: Can be optimized by querying summary data of middle days.
                FitbitService.getRequest(intraDayConverter, *FitbitApi.makeIntraDayRequestUrls(FitbitApi.REQUEST_INTRADAY_RESOURCE_PATH_DISTANCE, start, end))
                        as Observable<Nullable<out Any>>
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
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