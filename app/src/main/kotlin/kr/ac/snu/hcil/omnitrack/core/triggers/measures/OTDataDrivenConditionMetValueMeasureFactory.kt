package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTItemMetadataMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager

class OTDataDrivenConditionMetValueMeasureFactory(context: Context) : OTItemMetadataMeasureFactory(context, "dataDrivenConditionMetValue") {


    private val logicImpl = OTDataDrivenTriggerMetadataMeasureLogicImpl(context)

    override fun getAttributeType(): Int = OTAttributeManager.TYPE_NUMBER

    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override fun isAvailableToRequestValue(attribute: OTAttributeDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return logicImpl.isAvailableToRequestValue(attribute, invalidMessages)
    }

    override fun extractValueFromMetadata(metadata: JsonObject): Any? {
        return metadata.get(OTDataDrivenTriggerManager.METADATA_KEY_VALUE).asBigDecimal
    }

    override fun makeMeasure(): OTMeasure {
        return OTMetaDataMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return OTMetaDataMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return OTMetaDataMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override fun getCategoryName(): String = context.getString(R.string.msg_trigger_data_measure_category)

    override val nameResourceId: Int = R.string.msg_trigger_data_measure_value_name
    override val descResourceId: Int = R.string.msg_trigger_data_measure_value_description

}
