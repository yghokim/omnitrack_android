package kr.ac.snu.hcil.omnitrack.core.database.configured.models

import android.app.Activity
import android.support.annotation.ColorInt
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.Single
import io.realm.*
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import java.util.*

/**
 * Created by younghokim on 2017. 10. 1..
 */
open class OTTrackerDAO : RealmObject() {

    data class SimpleTrackerInfo(override val objectId: String?, val name: String, @ColorInt val color: Int) : IReadonlyObjectId

    companion object {
        val CREATION_FLAG_TUTORIAL: Map<String, String> by lazy {
            val result = HashMap<String, String>()
            result["source"] = "generated_example"
            result
        }

        val CREATION_FLAG_OPEN_OMNITRACK: Map<String, String> by lazy {
            val result = HashMap<String, String>()
            result["source"] = "generated_omnitrack_open_format"
            result
        }
    }

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

    fun makeAttributesQuery(inTrashcan:Boolean?=false, hidden: Boolean? = false): RealmQuery<OTAttributeDAO> {
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

    var serializedLockedPropertyInfo: String = "{}"

    var userCreatedAt: Long = System.currentTimeMillis()
    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.
    var userUpdatedAt: Long = System.currentTimeMillis()

    @Index
    var removed: Boolean = false

    @LinkingObjects("trackers")
    val triggers: RealmResults<OTTriggerDAO>? = null

    val liveTriggersQuery: RealmQuery<OTTriggerDAO>? get() = triggers?.where()?.equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)

    @Ignore
    private var _parsedCreationFlags: JsonObject? = null

    fun getParsedCreationFlags(): JsonObject {
        if (_parsedCreationFlags == null) {
            _parsedCreationFlags = CreationFlagsHelper.parseCreationFlags(serializedCreationFlags)
        }
        return _parsedCreationFlags!!
    }

    fun isExternalFilesInvolved(configuredContext: ConfiguredContext): Boolean {
        return getLiveAttributesSync().find { it.getHelper(configuredContext).isExternalFile(it) } != null
    }

    fun getLiveAttributesSync(): List<OTAttributeDAO> {
        return if (isManaged) {
            makeAttributesQuery(false, false).findAll()
        } else {
            attributes.filter { !it.isInTrashcan && !it.isHidden }
        }
    }

    fun getRequiredPermissions(configuredContext: ConfiguredContext): Array<String> {
        val list = ArrayList<String>()
        getLiveAttributesSync().forEach {
            try {
                val perms = it.getHelper(configuredContext).getRequiredPermissions(it)
                if (perms != null) {
                    list.addAll(perms)
                }
            } catch (ex: Exception) {

            }
        }

        return list.toTypedArray()
    }

    fun makePermissionAssertObservable(activity: Activity, configuredContext: ConfiguredContext): Observable<Boolean> {
        val requiredPermissions = this.getRequiredPermissions(configuredContext)
        return if (requiredPermissions.isNotEmpty()) {
            RxPermissions(activity).request(*requiredPermissions)
        } else {
            Observable.just(true)
        }
    }

    fun getSimpleInfo(): SimpleTrackerInfo {
        return SimpleTrackerInfo(objectId, name, color)
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

    fun getParsedConnection(): OTConnection? {
        return try {
            serializedConnection?.let { OTConnection.fromJson(it) }
        } catch (ex: JsonSyntaxException) {
            ex.printStackTrace(); null
        }
    }

    fun getHelper(configuredContext: ConfiguredContext): OTAttributeHelper {
        return configuredContext.configuredAppComponent.getAttributeManager().getAttributeHelper(type)
    }

    fun getInputViewType(configuredContext: ConfiguredContext, previewMode: Boolean): Int {
        return getHelper(configuredContext).getInputViewType(previewMode, this)
    }

    fun initialize(configuredContext: ConfiguredContext) {
        initializePropertiesWithDefaults(configuredContext)
        getHelper(configuredContext).initialize(this)
    }

    fun initializePropertiesWithDefaults(configuredContext: ConfiguredContext) {
        val attributeHelper = getHelper(configuredContext)
        attributeHelper.propertyKeys.forEach { key ->
            setPropertySerializedValue(key, attributeHelper.getPropertyHelper<Any>(key).getSerializedValue(attributeHelper.getPropertyInitialValue(key)!!))
        }
    }

    fun getFallbackValue(configuredContext: ConfiguredContext, realm: Realm): Single<Nullable<out Any>> {
        return getHelper(configuredContext).getFallbackValue(this, realm)
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