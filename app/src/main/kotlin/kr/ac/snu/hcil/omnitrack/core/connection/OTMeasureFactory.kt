package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.OTAttachableFactory
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTMeasureFactory(context: Context, factoryTypeName: String) : OTAttachableFactory<OTMeasureFactory.OTMeasure>(context, factoryTypeName) {

    abstract fun getAttributeType(): Int

    abstract val isRangedQueryAvailable: Boolean
    abstract val isDemandingUserInput: Boolean
    abstract val minimumGranularity: OTTimeRangeQuery.Granularity?

    /*** Typename in TypeStringSerializer
     *
     */
    abstract val dataTypeName: String

    abstract fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>>
    abstract fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>? = null): Boolean

    abstract class OTMeasure(factory: OTMeasureFactory, arguments: JsonObject?): OTAttachable(factory, arguments) {
        abstract fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>>
    }

    abstract class OTRangeQueriedMeasure(factory: OTMeasureFactory, arguments: JsonObject?) : OTMeasure(factory, arguments) {

        abstract fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>>

        override fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>> {
            val range = query!!.getRange(builder)
            return getValueRequest(range.first, range.second)
        }
    }
}