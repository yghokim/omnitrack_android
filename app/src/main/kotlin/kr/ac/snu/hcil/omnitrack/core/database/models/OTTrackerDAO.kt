package kr.ac.snu.hcil.omnitrack.core.database.models

import android.content.Context
import androidx.annotation.ColorInt
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.reactivex.Single
import io.realm.*
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTStringStringEntryDAO
import java.util.*

/**
 * Created by younghokim on 2017. 10. 1..
 */
open class OTTrackerDAO : RealmObject() {

    data class SimpleTrackerInfo(override val objectId: String?, val name: String, @ColorInt val color: Int, val reminders: Array<OTTriggerDAO.SimpleTriggerInfo>?) : IReadonlyObjectId

    @PrimaryKey
    var objectId: String? = null

    @Index
    var userId: String? = null

    var name: String = ""

    @Index
    var position: Int = 0

    var color: Int = 0
    var isBookmarked: Boolean = false

    var attributes = RealmList<OTAttributeDAO>()

    fun makeAttributesQuery(inTrashcan: Boolean? = false, hidden: Boolean? = false): RealmQuery<OTAttributeDAO> {
        var query = attributes.where()
        if (inTrashcan != null) {
            query = query.equalTo(BackendDbManager.FIELD_IS_IN_TRASHCAN, inTrashcan)
        }

        if (hidden != null) {
            query = query.equalTo(BackendDbManager.FIELD_IS_HIDDEN, inTrashcan)
        }

        return query
    }


    var serializedCreationFlags: String = "{}"

    @Index
    var experimentIdInFlags: String? = null

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

    @Ignore
    private var _parsedLockedPropertyInfo: JsonObject? = null

    fun getParsedLockedPropertyInfo(): JsonObject {
        if (_parsedLockedPropertyInfo == null) {
            _parsedLockedPropertyInfo = LockedPropertiesHelper.parseFlags(serializedLockedPropertyInfo)
        }
        return _parsedLockedPropertyInfo!!
    }

    fun isExternalFilesInvolved(context: Context): Boolean {
        return getLiveAttributesSync().find { it.getHelper(context).isExternalFile(it) } != null
    }

    fun getLiveAttributesSync(): List<OTAttributeDAO> {
        return if (isManaged) {
            makeAttributesQuery(false, false).findAll()
        } else {
            attributes.filter { !it.isInTrashcan && !it.isHidden }
        }
    }

    fun getRequiredPermissions(context: Context): Array<String> {
        val list = ArrayList<String>()
        getLiveAttributesSync().forEach {
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
        return SimpleTrackerInfo(objectId, name, color, if (populateReminders) {
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

    fun isEditingLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.COMMON_EDIT, getParsedLockedPropertyInfo())
    }

    fun isDeletionLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.COMMON_DELETE, getParsedLockedPropertyInfo())
    }

    fun isVisualizationLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_ENTER_VISUALIZATION, getParsedLockedPropertyInfo())
    }

    fun isItemListLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_ENTER_ITEM_LIST, getParsedLockedPropertyInfo())
    }


    fun isAddNewAttributeLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_ADD_NEW_ATTRIBUTE, getParsedLockedPropertyInfo())
    }

    fun isAddNewReminderLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_ADD_NEW_REMINDER, getParsedLockedPropertyInfo())
    }

    fun isIndependentInputLocked(): Boolean {
        return LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_SELF_INITIATED_INPUT, getParsedLockedPropertyInfo())
    }


    fun isInstantLoggingAvailable(): Boolean {
        for (attribute in getLiveAttributesSync()) {
            if (attribute.isRequired) {
                if (attribute.serializedConnection != null || attribute.fallbackValuePolicy != OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL) {
                    continue
                } else return false
            }
        }

        return true
    }
}


open class OTAttributeDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    @Index
    var localId: String = ""

    @Index
    var trackerId: String? = null

    var name: String = ""

    @Index
    var isHidden: Boolean = false
    @Index
    var isInTrashcan: Boolean = false

    var position: Int = 0
    var serializedConnection: String? = null
    var type: Int = -1
    var isRequired: Boolean = false
    var properties = RealmList<OTStringStringEntryDAO>()

    //If connection is specified, this policy is overriden.
    var fallbackValuePolicy: Int = DEFAULT_VALUE_POLICY_NULL
    var fallbackPresetSerializedValue: String? = null

    var userCreatedAt: Long = System.currentTimeMillis()
    var userUpdatedAt: Long = System.currentTimeMillis()

    var serializedCreationFlags: String = "{}"
    var serializedLockedPropertyInfo: String = "{}"

    fun getParsedConnection(context: Context): OTConnection? {
        return try {
            serializedConnection?.let { (context.applicationContext as OTAndroidApp).applicationComponent.getConnectionTypeAdapter().fromJson(it) }
        } catch (ex: JsonSyntaxException) {
            ex.printStackTrace(); null
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

    fun isEditingLocked(): Boolean {
        return LockedPropertiesHelper.isLocked(LockedPropertiesHelper.COMMON_EDIT, getParsedLockedPropertyInfo())
                ?: false
    }

    fun isDeletionLocked(): Boolean {
        return LockedPropertiesHelper.isLocked(LockedPropertiesHelper.COMMON_DELETE, getParsedLockedPropertyInfo())
                ?: false
    }

    fun isVisibilityLocked(): Boolean {
        return LockedPropertiesHelper.isLocked(LockedPropertiesHelper.ATTRIBUTE_VISIBILITY, getParsedLockedPropertyInfo())
                ?: false
    }

    fun getHelper(context: Context): OTAttributeHelper {
        return (context.applicationContext as OTAndroidApp).applicationComponent.getAttributeManager().getAttributeHelper(type)
    }

    fun getInputViewType(context: Context, previewMode: Boolean): Int {
        return getHelper(context).getInputViewType(previewMode, this)
    }

    fun initialize(context: Context) {
        initializePropertiesWithDefaults(context)
        getHelper(context).initialize(this)
    }

    fun initializePropertiesWithDefaults(context: Context) {
        val attributeHelper = getHelper(context)
        attributeHelper.propertyKeys.forEach { key ->
            setPropertySerializedValue(key, attributeHelper.getPropertyHelper<Any>(key).getSerializedValue(attributeHelper.getPropertyInitialValue(key)!!))
        }
    }

    fun getFallbackValue(context: Context, realm: Realm): Single<Nullable<out Any>> {
        return getHelper(context).getFallbackValue(this, realm)
    }

    fun setPropertySerializedValue(key: String, serializedValue: String): Boolean {
        var changed = false
        val match = properties.find { it.key == key }
        if (match != null) {
            if (match.value != serializedValue)
                changed = true

            match.value = serializedValue
        } else {
            properties.add(OTStringStringEntryDAO().apply {
                this.id = UUID.randomUUID().toString()
                this.key = key
                this.value = serializedValue
            })
            changed = true
        }

        return changed
    }

    fun getPropertySerializedValue(key: String): String? {
        return properties.find { it.key == key }?.value
    }


    companion object {

        const val DEFAULT_VALUE_POLICY_NULL = 0
        const val DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE = 1
        const val DEFAULT_VALUE_POLICY_FILL_WITH_PRESET = 2
        const val DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM = 3
    }
}