package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import rx.Observable
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
object RescueTimeComputerUsageDurationMeasureFactory : OTMeasureFactory("cud") {

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


    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override fun getService(): OTExternalService {
        return RescueTimeService
    }

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Hour
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


        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {
            return RescueTimeService.getSummaryRequest(Date(start), Date(end - 1), usageDurationCalculator) as Observable<Nullable<out Any>>
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is ComputerUsageDurationMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }

    }

}