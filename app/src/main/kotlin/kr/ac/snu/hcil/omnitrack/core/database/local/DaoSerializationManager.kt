package kr.ac.snu.hcil.omnitrack.core.database.local

import com.google.gson.TypeAdapter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.di.ForAttribute
import kr.ac.snu.hcil.omnitrack.core.di.ForItem
import kr.ac.snu.hcil.omnitrack.core.di.ForTracker
import kr.ac.snu.hcil.omnitrack.core.di.ForTrigger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-02.
 */
@Singleton
class DaoSerializationManager @Inject constructor(
        @ForTrigger val triggerTypeAdapter: Lazy<TypeAdapter<OTTriggerDAO>>,
        @ForAttribute val attributeTypeAdapter: Lazy<TypeAdapter<OTAttributeDAO>>,
        @ForTracker val trackerTypeAdapter: Lazy<TypeAdapter<OTTrackerDAO>>,
        @ForItem val itemTypeAdapter: Lazy<TypeAdapter<OTItemDAO>>
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

    fun serializeTracker(tracker: OTTrackerDAO): String {
        return trackerTypeAdapter.get().toJson(tracker)
    }

    fun parseTracker(trackerJson: String): OTTrackerDAO {
        return trackerTypeAdapter.get().fromJson(trackerJson)
    }

    fun serializeItem(item: OTItemDAO): String {
        return itemTypeAdapter.get().toJson(item)
    }

    fun parseItem(itemJson: String): OTItemDAO {
        return itemTypeAdapter.get().fromJson(itemJson)
    }
}