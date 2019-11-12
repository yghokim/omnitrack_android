package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTItemMetadataMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager

class OTDataDrivenConditionMetValueMeasureFactory(context: Context) : OTItemMetadataMeasureFactory(context, "dataDrivenConditionMetValue") {


    private val logicImpl = OTDataDrivenTriggerMetadataMeasureLogicImpl(context)

    override fun getAttributeType(): Int = OTFieldManager.TYPE_NUMBER

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return logicImpl.isAvailableToRequestValue(field, invalidMessages)
    }

    override fun extractValueFromMetadata(metadata: JsonObject): Any? {
        return metadata.get(OTDataDrivenTriggerManager.METADATA_KEY_VALUE)?.asBigDecimal
    }

    override fun makeAttachable(arguments: JsonObject?): OTMeasure {
        return OTMetaDataMeasure(this, arguments)
    }

    override fun getCategoryName(): String = context.getString(R.string.msg_trigger_data_measure_category)

    override val nameResourceId: Int = R.string.msg_trigger_data_measure_value_name
    override val descResourceId: Int = R.string.msg_trigger_data_measure_value_description

    override fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        return logicImpl.makeAvailabilityCheckObservable(field)
    }
}
