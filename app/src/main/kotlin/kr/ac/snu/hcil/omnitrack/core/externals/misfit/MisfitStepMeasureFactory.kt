package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho on 9/2/2016.
 */
object MisfitStepMeasureFactory : OTMeasureFactory("step") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override fun getService(): OTExternalService {
        return MisfitService
    }

    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Day
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return MisfitStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitStepMeasure(serialized)
    }

    override val nameResourceId: Int = R.string.measure_steps_name
    override val descResourceId: Int = R.string.measure_steps_desc


    class MisfitStepMeasure : OTRangeQueriedMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = MisfitStepMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>> {
            return Flowable.defer {
                val token = MisfitService.getStoredAccessToken()
                if (token != null) {
                    return@defer MisfitApi.getStepsOnDayRequest(token, Date(start), Date(end - 1)).toFlowable() as Flowable<Nullable<out Any>>
                } else {
                    return@defer Flowable.error<Nullable<out Any>>(Exception("no token"))
                }
            }
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is MisfitStepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}