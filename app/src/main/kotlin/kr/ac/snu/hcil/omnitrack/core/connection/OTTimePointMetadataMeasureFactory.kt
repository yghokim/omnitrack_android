package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.serialization.getLongCompat
import kr.ac.snu.hcil.omnitrack.core.types.TimePoint

abstract class OTTimePointMetadataMeasureFactory(context: Context, factoryTypeName: String) : OTItemMetadataMeasureFactory(context, factoryTypeName) {

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_TIMEPOINT
    override fun getAttributeType(): Int = OTFieldManager.TYPE_TIME

    protected abstract val metadataKey: String

    override fun extractValueFromMetadata(metadata: JsonObject): Any? {
        return metadata.getLongCompat(metadataKey)?.let {
            TimePoint(it, (context.applicationContext as OTAndroidApp).applicationComponent.getPreferredTimeZone().id)
        }
    }
}
