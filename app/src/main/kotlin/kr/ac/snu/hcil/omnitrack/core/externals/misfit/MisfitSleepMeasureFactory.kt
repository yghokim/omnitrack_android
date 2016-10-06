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
 * Created by Young-Ho Kim on 2016-09-01.
 */
object MisfitSleepMeasureFactory : OTMeasureFactory() {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE
    }

    override val service: OTExternalService = MisfitService

    override val exampleAttributeType: Int = OTAttribute.TYPE_TIMESPAN

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_TIMESPAN
    }

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return MisfitSleepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitSleepMeasure(serialized)
    }

    override val nameResourceId: Int = R.string.measure_misfit_sleeps_name
    override val descResourceId: Int = R.string.measure_misfit_sleeps_desc

    class MisfitSleepMeasure : OTRangeQueriedMeasure {

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

        override val factory: OTMeasureFactory = MisfitSleepMeasureFactory

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(start: Long, end: Long, handler: (Any?) -> Unit) {
            println("Grab Misfit sleep data")
            val token = MisfitService.getStoredAccessToken()
            if (token != null) {
                MisfitApi.getLatestSleepOnDayAsync(token, Date(start), Date(end - 20))
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