package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho on 9/2/2016.
 */
object MisfitStepMeasureFactory: OTMeasureFactory() {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val service: OTExternalService = MisfitService

    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return MisfitStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitStepMeasure(serialized)
    }

    override val nameResourceId: Int = R.string.measure_misfit_steps_name
    override val descResourceId: Int = R.string.measure_misfit_steps_desc


    class MisfitStepMeasure : OTRangeQueriedMeasure{

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = MisfitStepMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(start: Long, end: Long, handler: (Any?) -> Unit) {
            val token = MisfitService.getStoredAccessToken()
            if (token != null) {
                MisfitApi.getStepsOnDayAsync(token, Date(start), Date(end-1))
                {
                    result ->
                    println(result)
                    handler.invoke(result)
                }
            } else {
                handler.invoke(null)
            }
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

    }
}