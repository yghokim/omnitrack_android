package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import rx.Observable

/**
 * Created by Young-Ho Kim on 2016-10-06.
 */
object FitbitHeartRateMeasureFactory : OTMeasureFactory("heart") {

    override fun getService(): OTExternalService {
        return FitbitService
    }

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return FitbitHeartRateMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitHeartRateMeasure(serialized)
    }

    override val exampleAttributeType: Int
        get() = OTAttribute.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator = OTMeasureFactory.CONFIGURATOR_FOR_HEART_RATE_ATTRIBUTE

    override val nameResourceId: Int = R.string.measure_fitbit_heart_rate_name
    override val descResourceId: Int = R.string.measure_fitbit_heart_rate_desc

    class FitbitHeartRateMeasure : OTRangeQueriedMeasure {
        companion object {
            val converter = object : FitbitApi.AIntraDayConverter<Int, Int>("activities-heart-intraday") {
                override fun extractValueFromDatum(datum: JSONObject): Int {
                    return datum.getInt("value")
                }

                override fun processValues(values: List<Int>): Int {
                    return (values.average() + 0.5).toInt()
                }
            }
        }

        override fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>> {
            val urls = FitbitApi.makeIntraDayRequestUrls(FitbitApi.REQUEST_INTRADAY_RESOURCE_PATH_HEART_RATE, start, end)
            println(urls)
            return FitbitService.getRequest(converter, *urls) as Observable<Result<out Any>>
        }

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT
        override val factory: OTMeasureFactory = FitbitHeartRateMeasureFactory

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is FitbitHeartRateMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }

    }
}