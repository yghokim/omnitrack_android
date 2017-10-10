package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.*

/**
 * Created by Young-Ho on 10/8/2017.
 */
class AttributeDetailViewModel : ViewModel() {
    companion object {
        const val CONNECTION_NULL = "null"
    }

    var attributeDao: OTAttributeDAO? = null
        private set

    private val realm = OTApplication.app.databaseManager.getRealmInstance()

    val isValid: Boolean get() = attributeDao?.isValid ?: false

    val attributeHelper: OTAttributeHelper? get() = attributeDao?.let { OTAttributeManager.getAttributeHelper(it.type) }

    val isInDatabase: Boolean get() = attributeDao?.isManaged ?: false

    val nameObservable = BehaviorSubject.create<String>("")
    val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()

    val typeObservable = BehaviorSubject.create<Int>()

    val defaultValuePolicyObservable = BehaviorSubject.create<Int>(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)

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

    var defaultValuePolicy: Int
        get() = defaultValuePolicyObservable.value
        set(value) {
            if (value != defaultValuePolicyObservable.value) {
                defaultValuePolicyObservable.onNext(value)
            }
        }

    val isDefaultValuePolicyDirty: Boolean
        get() = defaultValuePolicy != attributeDao?.defaultValuePolicy

    val isNameDirty: Boolean
        get() = name != attributeDao?.name

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
        if (this.attributeDao != attributeDao) {
            this.attributeDao = attributeDao
                name = attributeDao.name
                connection = attributeDao.serializedConnection?.let { OTConnection.fromJson(it) }

                if (typeObservable.value != attributeDao.type)
                    typeObservable.onNext(attributeDao.type)

            defaultValuePolicy = attributeDao.defaultValuePolicy

                propertyTable.clear()
                val helper = attributeHelper
                if (helper != null) {
                    val table = helper.getDeserializedPropertyTable(attributeDao)
                        for (entry in table) {
                            propertyTable.set(entry.key, entry.value)
                            onPropertyValueChanged.onNext(Pair(entry.key, entry.value))
                        }
                }
        }
    }

    fun setPropertyValue(propertyKey: String, value: Any?) {
        if (propertyTable[propertyKey] != value) {
            propertyTable[propertyKey] = value
            onPropertyValueChanged.onNext(Pair(propertyKey, value))
        }
    }

    fun isPropertyChanged(propertyKey: String): Boolean {
        return attributeDao?.let { propertyTable[propertyKey] != attributeHelper?.getDeserializedPropertyValue(propertyKey, it) } ?: false
    }

    fun hasAnyPropertyChanged(): Boolean {
        return attributeHelper?.propertyKeys?.find { isPropertyChanged(it) } != null
    }

    fun isChanged(): Boolean {
        return isNameDirty || hasAnyPropertyChanged()
    }

    fun applyChanges() {
        attributeDao?.name = name
        attributeDao?.defaultValuePolicy = defaultValuePolicy

        for (entry in propertyTable) {
            val value = entry.value
            val propertyHelper = attributeHelper?.getPropertyHelper<Any>(entry.key)
            if (propertyHelper != null && value != null) {
                attributeDao?.setPropertySerializedValue(entry.key, propertyHelper.getSerializedValue(value))
            }
        }

        attributeDao?.serializedConnection = connection?.getSerializedString()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        attributeDao = null
    }
}