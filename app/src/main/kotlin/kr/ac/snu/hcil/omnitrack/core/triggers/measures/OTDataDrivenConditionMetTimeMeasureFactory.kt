package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimePointMetadataMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager

class OTDataDrivenConditionMetTimeMeasureFactory(context: Context) : OTTimePointMetadataMeasureFactory(context, "dataDrivenConditionMetTime") {

    private val logicImpl = OTDataDrivenTriggerMetadataMeasureLogicImpl(context)

    override val metadataKey: String = OTDataDrivenTriggerManager.METADATA_KEY_TIMESTAMP

    override fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return logicImpl.isAvailableToRequestValue(field, invalidMessages)
    }

    override fun makeAttachable(arguments: JsonObject?): OTMeasure {
        return OTMetaDataMeasure(this, arguments)
    }

    override fun getCategoryName(): String = context.getString(R.string.msg_trigger_data_measure_category)

    override val nameResourceId: Int = R.string.msg_trigger_data_measure_time_name
    override val descResourceId: Int = R.string.msg_trigger_data_measure_time_description

    override fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        return logicImpl.makeAvailabilityCheckObservable(field)
    }

}
