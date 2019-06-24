package kr.ac.snu.hcil.omnitrack.core.database

import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.typeadapters.ServerCompatibleTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.di.global.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-02.
 */
@Singleton
class DaoSerializationManager @Inject constructor(
        @ForTrigger val triggerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTriggerDAO>>,
        @ForAttribute val fieldTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTFieldDAO>>,
        @ForTracker val trackerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTrackerDAO>>,
        @ForItem val itemTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTItemDAO>>,
        @ForServerTrigger val serverTriggerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTriggerDAO>>,
        @ForServerAttribute val serverFieldTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTFieldDAO>>,
        @ForServerTracker val serverTrackerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTrackerDAO>>,
        @ForServerItem val serverItemTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTItemDAO>>
) {

    fun parseTrigger(triggerJson: String): OTTriggerDAO {
        return triggerTypeAdapter.get().fromJson(triggerJson)
    }

    fun serializeTrigger(trigger: OTTriggerDAO, isServerMode: Boolean = false): String {
        return (if (isServerMode) serverTriggerTypeAdapter else triggerTypeAdapter).get().toJson(trigger)
    }

    fun parseAttribute(attrJson: String): OTFieldDAO {
        return fieldTypeAdapter.get().fromJson(attrJson)
    }

    fun serializeAttribute(field: OTFieldDAO, isServerMode: Boolean = false): String {
        return (if (isServerMode) serverFieldTypeAdapter else fieldTypeAdapter).get().toJson(field)
    }

    fun serializeTracker(tracker: OTTrackerDAO, isServerMode: Boolean = false): String {
        return (if (isServerMode) serverTrackerTypeAdapter else trackerTypeAdapter).get().toJson(tracker)
    }

    fun parseTracker(trackerJson: String): OTTrackerDAO {
        return trackerTypeAdapter.get().fromJson(trackerJson)
    }

    fun serializeItem(item: OTItemDAO, isServerMode: Boolean = false): String {
        return (if (isServerMode) serverItemTypeAdapter else itemTypeAdapter).get().toJson(item)
    }

    fun parseItem(itemJson: String): OTItemDAO {
        return itemTypeAdapter.get().fromJson(itemJson)
    }
}