package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
class MisfitSleepMeasureFactory(context: Context, service: MisfitService) : OTServiceMeasureFactory(context, service, "slp") {

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMESPAN

    override fun getAttributeType(): Int = OTFieldManager.TYPE_TIMESPAN

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Day
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return MisfitSleepMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return MisfitSleepMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitSleepMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val nameResourceId: Int = R.string.measure_misfit_sleeps_name
    override val descResourceId: Int = R.string.measure_misfit_sleeps_desc

    class MisfitSleepMeasure(factory: MisfitSleepMeasureFactory) : OTRangeQueriedMeasure(factory) {

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {
            val service = getFactory<MisfitSleepMeasureFactory>().getService<MisfitService>()
            return Single.defer {
                val token = service.getStoredAccessToken()
                if (token != null) {
                    return@defer service.api.getLatestSleepOnDayRequest(token, Date(start), Date(end - 20))
                } else return@defer Single.error<Nullable<out Any>>(Exception("no token"))
            }
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