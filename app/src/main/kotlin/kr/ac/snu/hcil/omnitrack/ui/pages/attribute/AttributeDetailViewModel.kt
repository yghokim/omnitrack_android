package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
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
    val serializedConnectionObservable = BehaviorSubject.create<String>("")

    val typeObservable = BehaviorSubject.create<Int>()

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

    val isNameChanged: Boolean
        get() = name != attributeDao?.name

    var serializedConnection: String?
        get() {
            return serializedConnectionObservable.value
        }
        set(value) {
            if (serializedConnectionObservable.value != value) {
                serializedConnectionObservable.onNext(value)
            }
        }

    fun init(attributeDao: OTAttributeDAO) {
        if (this.attributeDao != attributeDao) {
            this.attributeDao = attributeDao
            if (attributeDao != null) {
                name = attributeDao.name
                serializedConnection = attributeDao.serializedConnection ?: CONNECTION_NULL

                if (typeObservable.value != attributeDao.type)
                    typeObservable.onNext(attributeDao.type)


                propertyTable.clear()
                val helper = attributeHelper
                if (helper != null) {
                    val table = helper.getDeserializedPropertyTable(attributeDao)
                    if (table != null) {
                        for (entry in table) {
                            propertyTable.set(entry.key, entry.value)
                            onPropertyValueChanged.onNext(Pair(entry.key, entry.value))
                        }
                    }
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
        return isNameChanged || hasAnyPropertyChanged()
    }

    fun applyChanges() {
        attributeDao?.name = name

        for (entry in propertyTable) {
            val value = entry.value
            val propertyHelper = attributeHelper?.getPropertyHelper<Any>(entry.key)
            if (propertyHelper != null && value != null) {
                attributeDao?.setPropertySerializedValue(entry.key, propertyHelper.getSerializedValue(value), realm)
            }
        }

        attributeDao?.serializedConnection = if (serializedConnection == CONNECTION_NULL) null else serializedConnection
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        attributeDao = null
    }
}