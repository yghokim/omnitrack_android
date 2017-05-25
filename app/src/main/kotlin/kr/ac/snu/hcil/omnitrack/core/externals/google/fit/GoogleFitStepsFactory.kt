package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 8/11/2016.
 */

object GoogleFitStepsFactory : GoogleFitService.GoogleFitMeasureFactory("step") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override fun getService(): OTExternalService {
        return GoogleFitService
    }

    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Millis
    override val isDemandingUserInput: Boolean = false

    override val nameResourceId: Int = R.string.measure_steps_name

    override val descResourceId: Int = R.string.measure_steps_desc

    override val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions> = Fitness.HISTORY_API
    override val usedScope: Scope = Fitness.SCOPE_ACTIVITY_READ

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute is OTNumberAttribute
    }

    override fun makeMeasure(): OTMeasure {
        return Measure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return Measure(serialized)
    }

    class Measure : OTRangeQueriedMeasure {

        override val dataTypeName = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = GoogleFitStepsFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>> {
            println("Requested Google Fit Step Measure")
            return if (factory.getService().state == OTExternalService.ServiceState.ACTIVATED) {
                GoogleFitService.getConnectedClient().map<Result<out Any>>(object : Func1<GoogleApiClient, Result<out Any>> {
                    override fun call(client: GoogleApiClient): Result<out Any> {
                        val request = DataReadRequest.Builder()
                            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                            .bucketByTime(1, TimeUnit.DAYS)
                            .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                            .build()
                        val result = Fitness.HistoryApi.readData(client, request).await(10, TimeUnit.SECONDS)
                        var steps = 0
                        for (bucket in result.buckets) {
                            for (dataset in bucket.dataSets) {
                                for (dataPoint in dataset.dataPoints) {
                                    steps += dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                    println(dataPoint.getValue(Field.FIELD_STEPS))
                            }
                            }
                    }

                        return Result(steps)
                    }
                }).subscribeOn(Schedulers.io())
            } else {
                Observable.error<Result<out Any>>(Exception("Service is not activated."))
            }
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {

        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is Measure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}