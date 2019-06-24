package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
class RescueTimeComputerUsageDurationMeasureFactory(context: Context, service: RescueTimeService) : OTServiceMeasureFactory(context, service, "cud") {

    override fun getAttributeType() = OTFieldManager.TYPE_NUMBER

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_DOUBLE
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Hour
    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return ComputerUsageDurationMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return ComputerUsageDurationMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return ComputerUsageDurationMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val nameResourceId: Int = R.string.measure_rescuetime_computer_usage_name
    override val descResourceId: Int = R.string.measure_rescuetime_computer_usage_desc

    val usageDurationCalculator = object : RescueTimeService.ISummaryCalculator<Double> {
        override fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): Double? {
            return if (list.isNotEmpty())
                list.asSequence().map { it.getDouble(RescueTimeService.SUMMARY_VARIABLE_TOTAL_HOURS) }.sum()
            else null
        }

    }

    class ComputerUsageDurationMeasure(factory: RescueTimeComputerUsageDurationMeasureFactory) : OTRangeQueriedMeasure(factory) {

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {
            val factory = getFactory<RescueTimeComputerUsageDurationMeasureFactory>()
            return factory.getService<RescueTimeService>().getSummaryRequest(Date(start), Date(end - 1),
                    factory.usageDurationCalculator) as Single<Nullable<out Any>>
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is ComputerUsageDurationMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }

    }

}