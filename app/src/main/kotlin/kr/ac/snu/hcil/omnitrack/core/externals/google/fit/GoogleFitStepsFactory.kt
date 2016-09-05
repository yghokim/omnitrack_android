package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import android.os.AsyncTask
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 8/11/2016.
 */

object GoogleFitStepsFactory : GoogleFitService.GoogleFitMeasureFactory() {

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val service: OTExternalService = GoogleFitService

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

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

        override val factory: OTMeasureFactory = GoogleFitStepsFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit) {
            val range = query?.getRange(builder)
            Task(range!!.first, range.second) {
                steps ->
                println("result step: $steps")
                handler.invoke(steps)
            }.execute()
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {

        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {

        }

        class Task(val from: Long, val to: Long, val handler: ((Int) -> Unit)?) : AsyncTask<Void?, Void?, Int>() {

            override fun doInBackground(vararg p0: Void?): Int {
                val request = DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(from, to, TimeUnit.MILLISECONDS)
                        .build()

                var finish = false

                var steps = 0

                GoogleFitService.getClientAsync {
                    client ->
                    Fitness.HistoryApi.readData(client!!, request).setResultCallback {
                        result ->
                        for (bucket in result.buckets) {
                            for (dataset in bucket.dataSets) {
                                for (dataPoint in dataset.dataPoints) {
                                    steps += dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                    println(dataPoint.getValue(Field.FIELD_STEPS))
                                }
                            }
                        }

                        finish = true
                    }
                }

                while (!finish) {
                }
                println("retreived steps")

                return steps
            }

            override fun onPostExecute(result: Int) {
                super.onPostExecute(result)
                handler?.invoke(result)
            }

        }

    }
}