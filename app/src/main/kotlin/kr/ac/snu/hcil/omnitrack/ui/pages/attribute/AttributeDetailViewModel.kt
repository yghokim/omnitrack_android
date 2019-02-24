package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.app.Application
import android.os.Bundle
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.configured.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import java.util.*
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/8/2017.
 */
class AttributeDetailViewModel(app: Application) : RealmViewModel(app) {
    companion object

    var isInitialized: Boolean = false
        private set


    var attributeDAO: OTAttributeDAO? = null
        private set

    val isValid: Boolean get() = attributeDAO?.isValid == true

    val attributeHelper: OTAttributeHelper? get() = attributeDAO?.let { attributeManager.getAttributeHelper(it.type) }

    val isInDatabase: Boolean get() = attributeDAO?.isManaged == true

    val nameObservable = BehaviorSubject.createDefault<String>("")
    val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()

    val typeObservable = BehaviorSubject.create<Int>()

    val defaultValuePolicyObservable = BehaviorSubject.createDefault(-1)
    val defaultValuePresetObservable = BehaviorSubject.createDefault<Nullable<String>>(Nullable(null))

    val isRequiredObservable = BehaviorSubject.createDefault<Boolean>(false)

    val onPropertyValueChanged = PublishSubject.create<Pair<String, Any?>>()

    private val propertyTable = Hashtable<String, Any?>()

    val currentPropertyTable: Map<String, Any?> get() = propertyTable

    var name: String
        get() {
            return nameObservable.value ?: ""
        }
        set(value) {
            nameObservable.onNextIfDifferAndNotNull(value)
        }

    var isRequired: Boolean
        get() {
            return isRequiredObservable.value ?: false
        }
        set(value) {
            isRequiredObservable.onNextIfDifferAndNotNull(value)
        }

    var defaultValuePolicy: Int
        get() = defaultValuePolicyObservable.value ?: -1
        set(value) {
            defaultValuePolicyObservable.onNextIfDifferAndNotNull(value)
        }

    var defaultValuePreset: String?
        get() = defaultValuePresetObservable.value?.datum
        set(value) {
            defaultValuePresetObservable.onNextIfDifferAndNotNull(Nullable(value))
        }

    val isConnectionDirty: Boolean
        get() = connectionObservable.value?.datum != attributeDAO?.getParsedConnection(getApplication())

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

    @Inject
    lateinit var attributeManager: OTAttributeManager

    @Inject
    lateinit var connectionTypeAdapter: OTConnection.ConnectionTypeAdapter

    @Inject
    lateinit var serializationManager: Lazy<DaoSerializationManager>

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    fun init(attributeDao: OTAttributeDAO, savedInstanceState: Bundle?) {
        if (!isInitialized) {
            this.attributeDAO = attributeDao

            val daoForFront = if (savedInstanceState != null) {
                val serializedFront = savedInstanceState.getString("frontDao")
                if (serializedFront != null) {
                    serializationManager.get().parseAttribute(serializedFront)
                } else attributeDao
            } else attributeDao

            name = daoForFront.name
            connection = daoForFront.serializedConnection?.let { connectionTypeAdapter.fromJson(it) }

            isRequired = daoForFront.isRequired

            defaultValuePolicy = daoForFront.fallbackValuePolicy
            defaultValuePreset = daoForFront.fallbackPresetSerializedValue

            propertyTable.clear()
            val helper = attributeHelper
            if (helper != null) {
                val table = helper.getDeserializedPropertyTable(daoForFront)
                for (entry in table) {
                    propertyTable.set(entry.key, entry.value)
                    onPropertyValueChanged.onNext(Pair(entry.key, entry.value))
                }
            }

            typeObservable.onNextIfDifferAndNotNull(daoForFront.type)

            isInitialized = true
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        val frontDao = makeFrontalChangesToDao()
        if (frontDao != null) {
            outState.putString("frontDao", serializationManager.get().serializeAttribute(frontDao, false))
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
            changes.add("connection" to connection?.getSerializedString(getApplication<OTApp>()))
        }

        return if (changes.isNotEmpty()) jsonObject(changes) else null
    }

    fun applyChanges() {
        println("state name: $name, required: $isRequired, dao: $attributeDAO")
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

        attributeDAO?.serializedConnection = connection?.getSerializedString(getApplication<OTApp>())
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

            dao.isRequired = isRequired
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

            dao.serializedConnection = connectionObservable.value?.datum?.getSerializedString(getApplication<OTApp>())
            dao
        }
    }

    override fun onCleared() {
        super.onCleared()
        attributeDAO = null
    }
}