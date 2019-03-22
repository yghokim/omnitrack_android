package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import com.google.gson.stream.JsonReader
import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimePointMetadataMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager

class OTDataDrivenConditionMetTimeMeasureFactory(context: Context) : OTTimePointMetadataMeasureFactory(context, "dataDrivenConditionMetTime") {

    private val logicImpl = OTDataDrivenTriggerMetadataMeasureLogicImpl(context)

    override val metadataKey: String = OTDataDrivenTriggerManager.METADATA_KEY_TIMESTAMP

    override fun isAvailableToRequestValue(attribute: OTAttributeDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return logicImpl.isAvailableToRequestValue(attribute, invalidMessages)
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

    override val nameResourceId: Int = R.string.msg_trigger_data_measure_time_name
    override val descResourceId: Int = R.string.msg_trigger_data_measure_time_description

    override fun makeAvailabilityCheckObservable(attribute: OTAttributeDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        return logicImpl.makeAvailabilityCheckObservable(attribute)
    }

}
