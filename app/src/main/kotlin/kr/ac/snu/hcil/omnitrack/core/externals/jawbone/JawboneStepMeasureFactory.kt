package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import com.google.gson.Gson
import com.jawbone.upplatformsdk.api.ApiManager
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import retrofit.RetrofitError
import retrofit.client.Response
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 2017. 1. 25..
 */
object JawboneStepMeasureFactory : OTMeasureFactory() {
    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false


    override fun makeMeasure(): OTMeasure {
        return JawboneStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return JawboneStepMeasure(serialized)
    }

    override val service: OTExternalService = JawboneUpService

    override val descResourceId: Int = R.string.measure_steps_desc
    override val nameResourceId: Int = R.string.measure_steps_name

    class JawboneStepMeasure : OTRangeQueriedMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = JawboneStepMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>> {
            if (TimeHelper.isSameDay(start, end - 10)) {
                return Observable.create<Result<out Any>> {
                    subscriber ->
                    ApiManager.getRestApiInterface().getMoveEventsList(UpPlatformSdkConstants.API_VERSION_STRING, HashMap<String, Long>().apply { this["date"] = JawboneUpService.makeFormattedDateInteger(start).toLong() }, object : retrofit.Callback<Any> {
                        override fun failure(error: RetrofitError) {
                            if (!subscriber.isUnsubscribed) {
                                subscriber.onError(error)
                            }
                        }

                        override fun success(result: Any, response: Response) {

                            if (!subscriber.isUnsubscribed) {
                                subscriber.onNext(Result(extractStepCountFromResult(result)))
                                subscriber.onCompleted()
                            }
                        }
                    })
                }
            } else {
                return Observable.create<Result<out Any>> {
                    subscriber ->

                    Observable.zip(TimeHelper.sliceToDate(start, end).map {
                        Observable.create<Int> {
                            subscriber ->
                            ApiManager.getRestApiInterface().getMoveEventsList(UpPlatformSdkConstants.API_VERSION_STRING, JawboneUpService.makeIntraDayRequestQueryParams(it.first, it.second, null), object : retrofit.Callback<Any> {
                                override fun failure(error: RetrofitError) {
                                    if (!subscriber.isUnsubscribed) {
                                        subscriber.onError(error)
                                    }
                                }

                                override fun success(result: Any, response: Response) {
                                    if (!subscriber.isUnsubscribed) {
                                        subscriber.onNext(extractStepCountFromResult(result))
                                        subscriber.onCompleted()
                                    }
                                }
                            })
                        }
                    }, { values ->
                        var sum = 0
                        for (value in values) {
                            if (value is Int) {
                                sum += value
                            }
                        }
                        sum
                    }).subscribe({
                        result ->
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(Result(result))
                            subscriber.onCompleted()
                        }
                    }, {
                        error ->
                        subscriber.onError(error)
                    })
                }
            }
        }

        private fun extractStepCountFromResult(result: Any): Int {
            try {
                val json = Gson().toJsonTree(result).asJsonObject
                val items = json.getAsJsonObject("data").getAsJsonArray("items")
                return items
                        .map { it -> it.asJsonObject }
                        .sumBy { it.getAsJsonObject("details").get("steps").asInt }
            } catch(exception: Exception) {
                exception.printStackTrace()
                return 0
            }
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is JawboneStepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}