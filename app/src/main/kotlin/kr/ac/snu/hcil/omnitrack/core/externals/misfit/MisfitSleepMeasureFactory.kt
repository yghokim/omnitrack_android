package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
object MisfitSleepMeasureFactory : OTMeasureFactory("slp") {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE
    }

    override fun getService(): OTExternalService {
        return MisfitService
    }

    override val exampleAttributeType: Int = OTAttributeManager.TYPE_TIMESPAN

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_TIMESPAN
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Day
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

        override fun getValueRequest(start: Long, end: Long): Observable<Nullable<out Any>> {
            return Observable.defer {
                val token = MisfitService.getStoredAccessToken()
                if (token != null) {
                    return@defer MisfitApi.getLatestSleepOnDayRequest(token, Date(start), Date(end - 20)) as Observable<Nullable<out Any>>
                } else return@defer Observable.error<Nullable<out Any>>(Exception("no token"))
            }
        }


        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is MisfitSleepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }

    }
}