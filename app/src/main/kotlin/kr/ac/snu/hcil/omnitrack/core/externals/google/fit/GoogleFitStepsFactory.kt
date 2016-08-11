package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.AsyncTaskWithResultHandler
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 8/11/2016.
 */

object GoogleFitStepsFactory : GoogleFitService.GoogleFitMeasureFactory() {
    override val service: OTExternalService = GoogleFitService

    override val isRangedQueryAvailable: Boolean = true

    override val nameResourceId: Int = R.string.measure_googlefit_steps_name

    override val descResourceId: Int = R.string.measure_googlefit_steps_desc

    override val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions> = Fitness.HISTORY_API
    override val usedScope: Scope = Fitness.SCOPE_ACTIVITY_READ

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        if (attribute is OTNumberAttribute) {
            return true
        } else return false
    }

    override fun makeMeasure(): OTMeasure {
        return Measure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return Measure(serialized)
    }

    class Measure : OTMeasure {
        override val dataTypeName = TypeStringSerializationHelper.TYPENAME_INT

        override val factoryCode: String = GoogleFitStepsFactory.typeCode

        override val factory: OTMeasureFactory = GoogleFitStepsFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(query: OTTimeRangeQuery?, handler: (Any) -> Unit) {

        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {

        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {

        }

    }

    class Task(handler: ((Boolean) -> Unit)?) : AsyncTaskWithResultHandler(handler) {
        override fun doInBackground(vararg p0: Void?): Boolean {
            val request = DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000, System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .build()

            var finish = false

            GoogleFitService.getClientAsync {
                client ->
                val result = Fitness.HistoryApi.readData(client!!, request).await()

                println("buckets = ${result.buckets.size}")

                for (bucket in result.buckets) {
                    for (dataset in bucket.dataSets) {
                        for (dataPoint in dataset.dataPoints) {
                            println(dataPoint.getValue(Field.FIELD_STEPS))
                        }
                    }
                }

                finish = true
            }

            while (!finish) {
            }

            return true
        }

    }
}