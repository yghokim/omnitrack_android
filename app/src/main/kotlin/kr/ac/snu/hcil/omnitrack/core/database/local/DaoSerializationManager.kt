package kr.ac.snu.hcil.omnitrack.core.database.local

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.AttributeTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.TrackerTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters.TriggerTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.di.ApplicationScope
import kr.ac.snu.hcil.omnitrack.core.di.ForAttribute
import kr.ac.snu.hcil.omnitrack.core.di.ForTracker
import kr.ac.snu.hcil.omnitrack.core.di.ForTrigger
import javax.inject.Inject

/**
 * Created by younghokim on 2017-11-02.
 */
@ApplicationScope
class DaoSerializationManager @Inject constructor(
        @ForTrigger val triggerTypeAdapter: Lazy<TypeAdapter<OTTriggerDAO>>,
        @ForAttribute val attributeTypeAdapter: Lazy<TypeAdapter<OTAttributeDAO>>,
        @ForTracker val trackerTypeAdapter: Lazy<TypeAdapter<OTTrackerDAO>>
) {

    fun parseTrigger(triggerJson: String): OTTriggerDAO {
        return triggerTypeAdapter.get().fromJson(triggerJson)
    }

    fun serializeTrigger(trigger: OTTriggerDAO): String {
        return triggerTypeAdapter.get().toJson(trigger)
    }

    fun parseAttribute(attrJson: String): OTAttributeDAO {
        return attributeTypeAdapter.get().fromJson(attrJson)
    }

    fun serializeAttribute(attribute: OTAttributeDAO): String{
        return attributeTypeAdapter.get().toJson(attribute)
    }
}