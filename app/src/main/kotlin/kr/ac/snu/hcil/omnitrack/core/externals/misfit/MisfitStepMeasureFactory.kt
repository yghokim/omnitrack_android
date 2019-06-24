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
 * Created by Young-Ho on 9/2/2016.
 */
class MisfitStepMeasureFactory(context: Context, service: MisfitService) : OTServiceMeasureFactory(context, service, "step") {

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

    override fun getAttributeType() = OTFieldManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = OTTimeRangeQuery.Granularity.Day
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return MisfitStepMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return MisfitStepMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return MisfitStepMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val nameResourceId: Int = R.string.measure_steps_name
    override val descResourceId: Int = R.string.measure_steps_desc


    class MisfitStepMeasure(factory: MisfitStepMeasureFactory) : OTRangeQueriedMeasure(factory) {

        override fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>> {
            return Single.defer {
                val service = getFactory<MisfitStepMeasureFactory>().getService<MisfitService>()
                val token = service.getStoredAccessToken()
                if (token != null) {
                    return@defer service.api.getStepsOnDayRequest(token, Date(start), Date(end - 1))
                } else {
                    return@defer Single.error<Nullable<out Any>>(Exception("no token"))
                }
            }
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