package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.app.Application
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import java.util.*

/**
 * Created by Young-Ho on 10/8/2017.
 */
class AttributeDetailViewModel(app: Application) : RealmViewModel(app) {
    companion object {
        const val CONNECTION_NULL = "null"
    }

    var isInitialized: Boolean = false
        private set


    var attributeDAO: OTAttributeDAO? = null
        private set

    val isValid: Boolean get() = attributeDAO?.isValid == true

    val attributeHelper: OTAttributeHelper? get() = attributeDAO?.let { OTAttributeManager.getAttributeHelper(it.type) }

    val isInDatabase: Boolean get() = attributeDAO?.isManaged == true

    val nameObservable = BehaviorSubject.createDefault<String>("")
    val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()

    val typeObservable = BehaviorSubject.create<Int>()

    val defaultValuePolicyObservable = BehaviorSubject.createDefault<Int>(-1)
    val defaultValuePresetObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable<String>(null))

    val isRequiredObservable = BehaviorSubject.createDefault<Boolean>(false)

    val onPropertyValueChanged = PublishSubject.create<Pair<String, Any?>>()

    private val propertyTable = Hashtable<String, Any?>()

    val currentPropertyTable: Map<String, Any?> get() = propertyTable

    var name: String
        get() {
            return nameObservable.value
        }
        set(value) {
            if (nameObservable.value != value) {
                nameObservable.onNext(value)
            }
        }

    var isRequired: Boolean
        get() {
            return isRequiredObservable.value
        }
        set(value) {
            if (isRequiredObservable.value != value) {
                isRequiredObservable.onNext(value)
            }
        }

    var defaultValuePolicy: Int
        get() = defaultValuePolicyObservable.value
        set(value) {
            if (value != defaultValuePolicyObservable.value) {
                defaultValuePolicyObservable.onNext(value)
            }
        }

    var defaultValuePreset: String?
        get() = defaultValuePresetObservable.value.datum
        set(value) {
            if (value != defaultValuePresetObservable.value.datum) {
                defaultValuePresetObservable.onNext(Nullable(value))
            }
        }

    val isConnectionDirty: Boolean
        get() = connectionObservable.value?.datum != attributeDAO?.getParsedConnection()

    val isDefaultValuePolicyDirty: Boolean
        get() = defaultValuePolicy != attributeDAO?.fallbackValuePolicy

    val isDefaultValuePresetDirty: Boolean
        get() = defaultValuePreset != attributeDAO?.fallbackPresetSerializedValue

    val isNameDirty: Boolean
        get() = name != attributeDAO?.name

    val isRequiredDirty: Boolean
        get() = isRequired != attributeDAO?.isRequired

    var connection: OTConnection?
        get() {
            return connectionObservable.value?.datum
        }
        set(value) {
            if (connectionObservable.value?.datum != value) {
                connectionObservable.onNext(Nullable(value))
            }
        }

    fun init(attributeDao: OTAttributeDAO) {
        if (!isInitialized) {
            this.attributeDAO = attributeDao
            name = attributeDao.name
            connection = attributeDao.serializedConnection?.let { OTConnection.fromJson(it) }

            isRequired = attributeDao.isRequired

            println("initial policy: ${attributeDao.fallbackValuePolicy}")

            defaultValuePolicy = attributeDao.fallbackValuePolicy
            defaultValuePreset = attributeDao.fallbackPresetSerializedValue

            propertyTable.clear()
            val helper = attributeHelper
            if (helper != null) {
                val table = helper.getDeserializedPropertyTable(attributeDao)
                for (entry in table) {
                    propertyTable.set(entry.key, entry.value)
                    onPropertyValueChanged.onNext(Pair(entry.key, entry.value))
                }
            }

            if (typeObservable.value != attributeDao.type)
                typeObservable.onNext(attributeDao.type)

            isInitialized = true
        }
    }

    fun setPropertyValue(propertyKey: String, value: Any?) {
        if (propertyTable[propertyKey] != value) {
            propertyTable[propertyKey] = value
            onPropertyValueChanged.onNext(Pair(propertyKey, value))
        }
    }

    fun isPropertyChanged(propertyKey: String): Boolean {
        return attributeDAO?.let { propertyTable[propertyKey] != attributeHelper?.getDeserializedPropertyValue<Any>(propertyKey, it) } == true
    }

    fun getOriginalPropertyValue(propertyKey: String): Any? {
        return attributeDAO?.let { attributeHelper?.getDeserializedPropertyValue<Any>(propertyKey, it) }
    }

    fun hasAnyPropertyChanged(): Boolean {
        return attributeHelper?.propertyKeys?.find { isPropertyChanged(it) } != null
    }

    fun isChanged(): Boolean {
        return isNameDirty || isRequiredDirty || isDefaultValuePolicyDirty || isDefaultValuePresetDirty || hasAnyPropertyChanged() || isConnectionDirty
    }

    fun makeDirtySignature(): JsonObject? {
        val changes = ArrayList<Pair<String, *>>()
        if (isNameDirty) {
            changes.add("name" to name)
        }

        if (isRequiredDirty) {
            changes.add("isRequired" to isRequired)
        }

        if (isDefaultValuePolicyDirty) {
            changes.add("fallbackValuePolicy" to defaultValuePolicy)
        }

        if (isDefaultValuePresetDirty) {
            changes.add("fallbackPreset" to defaultValuePreset)
        }

        if (hasAnyPropertyChanged()) {
            changes.add("properties" to true)
        }

        if (isConnectionDirty) {
            changes.add("connection" to connection?.getSerializedString())
        }

        return if (changes.isNotEmpty()) jsonObject(changes) else null
    }

    fun applyChanges() {
        attributeDAO?.name = name
        attributeDAO?.fallbackValuePolicy = defaultValuePolicy
        attributeDAO?.fallbackPresetSerializedValue = defaultValuePreset
        attributeDAO?.isRequired = isRequired

        for (entry in propertyTable) {
            val value = entry.value
            val propertyHelper = attributeHelper?.getPropertyHelper<Any>(entry.key)
            if (propertyHelper != null && value != null) {
                attributeDAO?.setPropertySerializedValue(entry.key, propertyHelper.getSerializedValue(value))
            }
        }

        attributeDAO?.serializedConnection = connection?.getSerializedString()
    }

    fun makeFrontalChangesToDao(): OTAttributeDAO? {
        return attributeDAO?.let {
            val dao = OTAttributeDAO()
            dao.objectId = it.objectId
            dao.type = it.type
            dao.localId = it.localId
            dao.position = it.position
            dao.trackerId = it.trackerId
            dao.userUpdatedAt = it.userUpdatedAt
            dao.isRequired = it.isRequired
            dao.fallbackValuePolicy = defaultValuePolicy
            dao.fallbackPresetSerializedValue = defaultValuePreset
            dao.name = name
            for (entry in propertyTable) {
                val value = entry.value
                val propertyHelper = attributeHelper?.getPropertyHelper<Any>(entry.key)
                if (propertyHelper != null && value != null) {
                    dao.setPropertySerializedValue(entry.key, propertyHelper.getSerializedValue(value))
                }
            }

            dao.serializedConnection = connectionObservable.value?.datum?.getSerializedString()
            dao
        }
    }

    override fun onCleared() {
        super.onCleared()
        attributeDAO = null
    }
}