package kr.ac.snu.hcil.omnitrack.core.database.models

import android.content.Context
import androidx.annotation.ColorInt
import com.google.gson.JsonObject
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTrackerLayoutElementDAO
import kr.ac.snu.hcil.omnitrack.core.flags.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.flags.F
import kr.ac.snu.hcil.omnitrack.core.flags.LockFlagLevel
import kr.ac.snu.hcil.omnitrack.core.flags.LockedPropertiesHelper
import java.util.*

/**
 * Created by younghokim on 2017. 10. 1..
 */
open class OTTrackerDAO : RealmObject() {

    data class SimpleTrackerInfo(override val _id: String?, val name: String, @ColorInt val color: Int, val reminders: Array<OTTriggerDAO.SimpleTriggerInfo>?) : IReadonlyObjectId

    @Suppress("PropertyName")
    @PrimaryKey
    var _id: String? = null

    @Index
    var userId: String? = null

    var name: String = ""

    @Index
    var position: Int = 0

    var color: Int = 0
    var isBookmarked: Boolean = false

    var fields = RealmList<OTFieldDAO>()

    var descriptionPanels: RealmList<OTDescriptionPanelDAO>? = null

    var layout: RealmList<OTTrackerLayoutElementDAO>? = null

    fun makeAttributesQuery(inTrashcan: Boolean? = false, hidden: Boolean? = false): RealmQuery<OTFieldDAO> {
        var query = fields.where()
        if (inTrashcan != null) {
            query = query.equalTo(BackendDbManager.FIELD_IS_IN_TRASHCAN, inTrashcan)
        }

        if (hidden != null) {
            query = query.equalTo(BackendDbManager.FIELD_IS_HIDDEN, inTrashcan)
        }

        return query
    }


    var serializedCreationFlags: String = "{}"

    var serializedLockedPropertyInfo: String = "{}"

    var userCreatedAt: Long = System.currentTimeMillis()
    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.
    var userUpdatedAt: Long = System.currentTimeMillis()

    var redirectUrl: String? = null

    @Index
    var removed: Boolean = false

    @LinkingObjects("trackers")
    val triggers: RealmResults<OTTriggerDAO>? = null

    val liveTriggersQuery: RealmQuery<OTTriggerDAO>? get() = triggers?.where()?.equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)

    @Ignore
    private var _parsedCreationFlags: JsonObject? = null

    fun clearCreationFlagsCache() {
        _parsedCreationFlags = null
    }

    fun getParsedCreationFlags(): JsonObject {
        if (_parsedCreationFlags == null) {
            _parsedCreationFlags = CreationFlagsHelper.parseFlags(serializedCreationFlags)
        }
        return _parsedCreationFlags!!
    }

    fun initializeUserCreatedTracker() {
        val flags = LockedPropertiesHelper.generateDefaultFlags(LockFlagLevel.Tracker, true)
        this._parsedLockedPropertyInfo = flags
        this.serializedLockedPropertyInfo = flags.toString()


        if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
            this.serializedCreationFlags = CreationFlagsHelper.Builder().setExperiment(BuildConfig.DEFAULT_EXPERIMENT_ID).build()
        }
    }

    @Ignore
    private var _parsedLockedPropertyInfo: JsonObject? = null

    fun getParsedLockedPropertyInfo(): JsonObject {
        if (_parsedLockedPropertyInfo == null) {
            _parsedLockedPropertyInfo = LockedPropertiesHelper.parseFlags(serializedLockedPropertyInfo)
        }
        return _parsedLockedPropertyInfo!!
    }

    fun isExternalFilesInvolved(context: Context): Boolean {
        return getLiveFieldsSync().find { it.getHelper(context).isExternalFile(it) } != null
    }

    fun getLiveFieldsSync(): List<OTFieldDAO> {
        return if (isManaged) {
            makeAttributesQuery(false, false).findAll()
        } else {
            fields.filter { !it.isInTrashcan && !it.isHidden }
        }
    }

    fun getRequiredPermissions(context: Context): Array<String> {
        val list = ArrayList<String>()
        getLiveFieldsSync().forEach {
            try {
                val perms = it.getHelper(context).getRequiredPermissions(it)
                if (perms != null) {
                    list.addAll(perms)
                }
            } catch (ex: Exception) {

            }
        }

        return list.toTypedArray()
    }

    fun getSimpleInfo(populateReminders: Boolean = false): SimpleTrackerInfo {
        return SimpleTrackerInfo(_id, name, color, if (populateReminders) {
            if (isManaged) {
                liveTriggersQuery?.equalTo("actionType", OTTriggerDAO.ACTION_TYPE_REMIND)?.findAll()
            } else {
                triggers?.filter { it.removed == false && it.actionType == OTTriggerDAO.ACTION_TYPE_REMIND }
            }?.map { it.getSimpleInfo(false) }
                    ?.toTypedArray() ?: emptyArray()
        } else {
            null
        })
    }

    fun isEditingAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.Modify, getParsedLockedPropertyInfo())
    }

    fun isRemovalAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.Delete, getParsedLockedPropertyInfo())
    }

    fun isVisualizationAccessAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.AccessVisualization, getParsedLockedPropertyInfo())
    }

    fun isItemListAccessAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.AccessItems, getParsedLockedPropertyInfo())
    }

    fun isAddNewFieldsAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.AddNewFields, getParsedLockedPropertyInfo())
    }

    fun isAddNewRemindersAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.AddNewReminders, getParsedLockedPropertyInfo())
    }

    fun isManualInputAllowed(): Boolean {
        return LockedPropertiesHelper.flag(LockFlagLevel.Tracker, F.ManualInput, getParsedLockedPropertyInfo())
    }

    fun isInstantLoggingAvailable(): Boolean {
        for (field in getLiveFieldsSync()) {
            if (field.isRequired) {
                if (field.serializedConnection != null || field.fallbackValuePolicy != OTFieldDAO.DEFAULT_VALUE_POLICY_NULL) {
                    continue
                } else return false
            }
        }

        return true
    }
}


