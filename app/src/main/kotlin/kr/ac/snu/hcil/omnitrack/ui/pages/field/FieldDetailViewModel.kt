package kr.ac.snu.hcil.omnitrack.ui.pages.field

import android.app.Application
import android.os.Bundle
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTFieldHelper
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import java.util.*
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/8/2017.
 */
class FieldDetailViewModel(app: Application) : RealmViewModel(app) {
    companion object

    var isInitialized: Boolean = false
        private set


    var fieldDAO: OTFieldDAO? = null
        private set

    val isValid: Boolean get() = fieldDAO?.isValid == true

    val fieldHelper: OTFieldHelper? get() = fieldDAO?.let { fieldManager.get(it.type) }

    val isInDatabase: Boolean get() = fieldDAO?.isManaged == true

    val nameObservable = BehaviorSubject.createDefault<String>("")
    val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()

    val typeObservable = BehaviorSubject.create<Int>()

    val defaultValuePolicyObservable = BehaviorSubject.createDefault(OTFieldDAO.DEFAULT_VALUE_POLICY_NULL)
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

    var defaultValuePolicy: String
        get() = defaultValuePolicyObservable.value ?: OTFieldDAO.DEFAULT_VALUE_POLICY_NULL
        set(value) {
            defaultValuePolicyObservable.onNextIfDifferAndNotNull(value)
        }

    var defaultValuePreset: String?
        get() = defaultValuePresetObservable.value?.datum
        set(value) {
            defaultValuePresetObservable.onNextIfDifferAndNotNull(Nullable(value))
        }

    val isConnectionDirty: Boolean
        get() = connectionObservable.value?.datum != fieldDAO?.getParsedConnection(getApplication())

    val isDefaultValuePolicyDirty: Boolean
        get() = defaultValuePolicy != fieldDAO?.fallbackValuePolicy

    val isDefaultValuePresetDirty: Boolean
        get() = defaultValuePreset != fieldDAO?.fallbackPresetSerializedValue

    val isNameDirty: Boolean
        get() = name != fieldDAO?.name

    val isRequiredDirty: Boolean
        get() = isRequired != fieldDAO?.isRequired

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
    lateinit var fieldManager: OTFieldManager

    @Inject
    lateinit var connectionTypeAdapter: OTConnection.ConnectionTypeAdapter

    @Inject
    lateinit var serializationManager: Lazy<DaoSerializationManager>

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    fun init(fieldDao: OTFieldDAO, savedInstanceState: Bundle?) {
        if (!isInitialized) {
            this.fieldDAO = fieldDao

            val daoForFront = if (savedInstanceState != null) {
                val serializedFront = savedInstanceState.getString("frontDao")
                if (serializedFront != null) {
                    serializationManager.get().parseAttribute(serializedFront)
                } else fieldDao
            } else fieldDao

            name = daoForFront.name
            connection = daoForFront.serializedConnection?.let { connectionTypeAdapter.fromJson(it) }

            isRequired = daoForFront.isRequired

            defaultValuePolicy = daoForFront.fallbackValuePolicy
            defaultValuePreset = daoForFront.fallbackPresetSerializedValue

            propertyTable.clear()
            val helper = fieldHelper
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
        return fieldDAO?.let { propertyTable[propertyKey] != fieldHelper?.getDeserializedPropertyValue<Any>(propertyKey, it) } == true
    }

    fun getOriginalPropertyValue(propertyKey: String): Any? {
        return fieldDAO?.let { fieldHelper?.getDeserializedPropertyValue<Any>(propertyKey, it) }
    }

    fun hasAnyPropertyChanged(): Boolean {
        return fieldHelper?.propertyKeys?.find { isPropertyChanged(it) } != null
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
        println("state name: $name, required: $isRequired, dao: $fieldDAO")
        fieldDAO?.name = name
        fieldDAO?.fallbackValuePolicy = defaultValuePolicy
        fieldDAO?.fallbackPresetSerializedValue = defaultValuePreset
        fieldDAO?.isRequired = isRequired

        for (entry in propertyTable) {
            val value = entry.value
            val propertyHelper = fieldHelper?.getPropertyHelper<Any>(entry.key)
            if (propertyHelper != null && value != null) {
                fieldDAO?.setPropertySerializedValue(entry.key, propertyHelper.getSerializedValue(value))
            }
        }

        fieldDAO?.serializedConnection = connection?.getSerializedString(getApplication<OTApp>())
    }

    fun makeFrontalChangesToDao(): OTFieldDAO? {
        return fieldDAO?.let {
            val dao = OTFieldDAO()
            dao._id = it._id
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
                val propertyHelper = fieldHelper?.getPropertyHelper<Any>(entry.key)
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
        fieldDAO = null
    }
}