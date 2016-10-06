package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
object RescueTimeComputerUsageDurationMeasureFactory : OTMeasureFactory() {

    val configurator = object : IExampleAttributeConfigurator {
        override fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean {
            if (attr is OTNumberAttribute) {
                val ns = NumberStyle()
                ns.unit = "Hour"
                ns.fractionPart = 2
                ns.commaUnit = 0
                ns.pluralizeUnit = true
                ns.unitPosition = NumberStyle.UnitPosition.Rear
                return true
            } else return false
        }
    }

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return configurator
    }


    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val service: OTExternalService = RescueTimeService

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return ComputerUsageDurationMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return ComputerUsageDurationMeasure(serialized)
    }

    override val nameResourceId: Int = R.string.measure_rescuetime_computer_usage_name
    override val descResourceId: Int = R.string.measure_rescuetime_computer_usage_desc

    val usageDurationCalculator = object : RescueTimeService.ISummaryCalculator<Double> {
        override fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): Double? {
            return if (list.size > 0)
                list.map { it.getDouble(RescueTimeService.SUMMARY_VARIABLE_TOTAL_HOURS) }.sum()
            else null
        }

    }

    class ComputerUsageDurationMeasure : OTRangeQueriedMeasure {
        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_DOUBLE
        override val factory: OTMeasureFactory = RescueTimeComputerUsageDurationMeasureFactory

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(start:Long, end: Long, handler: (Any?) -> Unit) {

            RescueTimeService.requestSummary(Date(start), Date(end - 1), usageDurationCalculator) {
                result ->
                handler.invoke(result)
            }
/*
            val apiKey = RescueTimeService.getStoredApiKey()

            if (apiKey != null) {
                RescueTimeApi.queryUsageDuration(RescueTimeApi.Mode.ApiKey, Date(start), Date(end-1))
                {
                    result ->
                    println(result)
                    handler.invoke(result)
                }
            } else {
                handler.invoke(null)
            }*/
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)

    }

}