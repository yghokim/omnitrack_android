package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper

class OTUnSupportedDummyService(context: Context, pref: SharedPreferences) : OTExternalService(context, pref, "UnSupported", 0) {
    override val requiredApiKeyNames: Array<String> by lazy { emptyArray<String>() }

    override fun isSupportedInSystem(serviceManager: OTExternalServiceManager): Boolean = true

    override fun onRegisterMeasureFactories(): Array<OTServiceMeasureFactory> = arrayOf(OTUnSupportedDummyMeasureFactory(context, this))

    override fun onDeactivate(): Completable = Completable.complete()

    override val thumbResourceId: Int = 0
    override val nameResourceId: Int = 0
    override val descResourceId: Int = 0

    class OTUnSupportedDummyMeasureFactory(context: Context, parentService: OTUnSupportedDummyService) : OTServiceMeasureFactory(context, parentService, "Dummy") {

        override fun getAttributeType(): Int = -1

        override val isRangedQueryAvailable: Boolean = false

        override val isDemandingUserInput: Boolean = false

        override val minimumGranularity: OTTimeRangeQuery.Granularity? = null
        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_STRING


        override fun makeMeasure(arguments: JsonObject?): OTMeasure {
            throw UnsupportedOperationException("This method is unused for UnSupportedDummyMeasure.")
        }

        fun makeMeasure(factoryCode: String, arguments: JsonObject?): DummyMeasure {
            return DummyMeasure(this, factoryCode, arguments)
        }

        override val nameResourceId: Int = R.string.msg_external_service_unsupported_measure
        override val descResourceId: Int = 0

        override fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
            invalidMessages?.add(context.getString(R.string.msg_external_service_unsupported_measure))
            return false
        }

        override fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
            return Observable.just(Pair(false, listOf(context.getString(R.string.msg_external_service_unsupported_measure))))
        }

        override fun onMakeFormattedName(): String {
            return context.getString(R.string.msg_external_service_unsupported_measure)
        }
    }

    class DummyMeasure(factory: OTUnSupportedDummyMeasureFactory, val originalFactoryCode: String, arguments: JsonObject?) : OTMeasureFactory.OTMeasure(factory, arguments) {
        override val factoryCode: String
            get() = originalFactoryCode

        override fun getFormattedName(): CharSequence {
            return "${getFactory<OTUnSupportedDummyMeasureFactory>().context.getString(R.string.msg_external_service_unsupported_measure)} (${originalFactoryCode})"
        }

        override fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>> {
            return Single.just(Nullable(null))
        }

        override fun equals(other: Any?): Boolean {
            return if (other is DummyMeasure) {
                other.originalFactoryCode == originalFactoryCode && other.arguments == arguments
            } else if (other is OTMeasureFactory.OTMeasure) {
                other.factoryCode == originalFactoryCode && other.arguments == arguments
            } else false
        }
    }
}