package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
object MisfitSleepFactory : OTMeasureFactory() {
    override val service: OTExternalService = MisfitService

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_TIMESPAN
    }

    override val isRangedQueryAvailable: Boolean = true

    override fun makeMeasure(): OTMeasure {
        return MisfitSleepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitSleepMeasure(serialized)
    }

    override val nameResourceId: Int = R.string.measure_misfit_sleeps_name
    override val descResourceId: Int = R.string.measure_misfit_sleeps_desc

    class MisfitSleepMeasure : OTMeasure {

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

        override val factoryCode: String = MisfitSleepFactory.typeCode

        override val factory: OTMeasureFactory = MisfitSleepFactory

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit) {
            println("Grab Misfit sleep data")
            val range = query!!.getRange(builder)
            val token = MisfitService.getStoredAccessToken()
            if (token != null) {
                MisfitApi.getLatestSleepOnDayAsync(token, Date(range.first), Date(range.second - 20))
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