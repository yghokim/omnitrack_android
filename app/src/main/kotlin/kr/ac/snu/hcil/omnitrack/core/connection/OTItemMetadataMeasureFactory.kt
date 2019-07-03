package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase

abstract class OTItemMetadataMeasureFactory(context: Context, factoryTypeName: String) : OTMeasureFactory(context, factoryTypeName) {

    final override val isRangedQueryAvailable: Boolean = false
    override val minimumGranularity: OTTimeRangeQuery.Granularity? = null
    override val isDemandingUserInput: Boolean = false
    override val typeCode: String by lazy {
        "itemmetadata_$factoryTypeName"
    }

    protected abstract fun extractValueFromMetadata(metadata: JsonObject): Any?

    open class OTMetaDataMeasure(factory: OTItemMetadataMeasureFactory, arguments: JsonObject?) : OTMeasure(factory, arguments) {
        override fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>> {
            val serializedMetadata = builder?.dao?.serializedMetadata
            if (serializedMetadata != null) {
                val metadata = (builder.context.applicationContext as OTAndroidApp).applicationComponent.genericGson().fromJson(serializedMetadata, JsonObject::class.java)
                return Single.just(Nullable(getFactory<OTItemMetadataMeasureFactory>().extractValueFromMetadata(metadata)))
            } else {
                return Single.just(Nullable(null))
            }
        }
    }
}