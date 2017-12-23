package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.gson.stream.JsonReader
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
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

    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Millis
    override val isDemandingUserInput: Boolean = false

    override val nameResourceId: Int = R.string.measure_steps_name

    override val descResourceId: Int = R.string.measure_steps_desc

    override val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions> = Fitness.HISTORY_API
    override val usedScope: Scope = Fitness.SCOPE_ACTIVITY_READ

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override fun makeMeasure(): OTMeasure {
        return Measure()
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return Measure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return Measure()
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    class Measure : OTRangeQueriedMeasure() {

        override val dataTypeName = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = GoogleFitStepsFactory

        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {
            println("Requested Google Fit Step Measure")
            return if (factory.getService().state == OTExternalService.ServiceState.ACTIVATED) {
                GoogleFitService.getConnectedClient().toFlowable(BackpressureStrategy.LATEST).map<Nullable<out Any>> { client ->
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

                    return@map Nullable(steps)
                }.subscribeOn(Schedulers.io())
            } else {
                Flowable.error<Nullable<out Any>>(Exception("Service is not activated."))
            }
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