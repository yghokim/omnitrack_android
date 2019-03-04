package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
class RescueTimeComputerUsageDurationMeasureFactory(context: Context, service: RescueTimeService) : OTServiceMeasureFactory(context, service, "cud") {

    val configurator = object : IExampleAttributeConfigurator {
        override fun configureExampleAttribute(attr: OTAttributeDAO): Boolean {
            /*if (attr is OTNumberAttribute) {
                val ns = NumberStyle()
                ns.unit = "Hour"
                ns.fractionPart = 2
                ns.commaUnit = 0
                ns.pluralizeUnit = true
                ns.unitPosition = NumberStyle.UnitPosition.Rear
                return true
            } else*/ return false
        }
    }

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return configurator
    }


    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_DOUBLE
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
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

        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {
            val factory = getFactory<RescueTimeComputerUsageDurationMeasureFactory>()
            return factory.getService<RescueTimeService>().getSummaryRequest(Date(start), Date(end - 1),
                    factory.usageDurationCalculator).toFlowable() as Flowable<Nullable<out Any>>
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