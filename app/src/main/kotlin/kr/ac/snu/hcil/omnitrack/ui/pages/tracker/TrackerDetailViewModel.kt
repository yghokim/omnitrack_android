package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.support.annotation.DrawableRes
import android.support.v7.util.DiffUtil
import io.realm.Realm
import io.realm.RealmChangeListener
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.move
import org.jetbrains.anko.collections.forEachWithIndex
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.*
import kotlin.collections.HashSet

/**
 * Created by younghokim on 2017. 10. 3..
 */
class TrackerDetailViewModel : RealmViewModel() {

    private var trackerDao: OTTrackerDAO? = null

    val trackerId: String? get() = this.trackerDao?.objectId

    //Observables========================================
    val nameObservable = BehaviorSubject.create<String>("")
    val isBookmarkedObservable = BehaviorSubject.create<Boolean>(false)
    val colorObservable = BehaviorSubject.create<Int>(OTApplication.app.colorPalette[0])
    val attributeViewModelListObservable = BehaviorSubject.create<List<AttributeInformationViewModel>>()
    //===================================================

    var name: String
        get() = nameObservable.value
        set(value) {
            if (value != nameObservable.value) {
                nameObservable.onNext(value)
            }
        }

    var isBookmarked: Boolean
        get() = isBookmarkedObservable.value
        set(value) {
            if (value != isBookmarkedObservable.value) {
                isBookmarkedObservable.onNext(value)
            }
        }

    var color: Int
        get() = colorObservable.value
        set(value) {
            if (value != colorObservable.value) {
                colorObservable.onNext(value)
            }
        }

    fun getAttributeViewModelList(): List<AttributeInformationViewModel> {
        return currentAttributeViewModelList
    }

    private val removedAttributes = HashSet<AttributeInformationViewModel>()

    private val currentAttributeViewModelList = ArrayList<AttributeInformationViewModel>()

    fun init(trackerId: String?) {
        if (trackerDao?.objectId != trackerId) {
            subscriptions.clear()
            if (trackerId != null) {
                val dao = OTApplication.app.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirstAsync()

                subscriptions.add(
                        dao.asObservable<OTTrackerDAO>().filter { it.isValid }.first().subscribe { snapshot ->

                            name = snapshot.name
                            isBookmarked = snapshot.isBookmarked
                            color = snapshot.color

                            clearCurrentAttributeList()
                            currentAttributeViewModelList.addAll(snapshot.attributes.map { AttributeInformationViewModel(it, realm) })
                            attributeViewModelListObservable.onNext(currentAttributeViewModelList)
                        }
                )

                trackerDao = dao

                /*
                attributeRealmResults?.removeChangeListener(attributeListChangedListener)
                attributeRealmResults = OTApplication.app.databaseManager.getAttributeListQuery(trackerId, realm).findAllSortedAsync("position")
                attributeRealmResults?.addChangeListener(attributeListChangedListener)*/

            } else trackerDao = null
        }
    }

    fun addNewAttribute(name: String, type: Int, processor: ((OTAttributeDAO, Realm) -> OTAttributeDAO)? = null) {
        val newDao = OTAttributeDAO()
        newDao.objectId = UUID.randomUUID().toString()
        newDao.name = name
        newDao.type = type
        newDao.trackerId = trackerId
        newDao.initializePropertiesWithDefaults(realm)
        processor?.invoke(newDao, realm)

        currentAttributeViewModelList.add(AttributeInformationViewModel(newDao, realm))
        attributeViewModelListObservable.onNext(currentAttributeViewModelList)
    }

    fun moveAttribute(from: Int, to: Int) {
        currentAttributeViewModelList.move(from, to)
        attributeViewModelListObservable.onNext(currentAttributeViewModelList)
    }

    fun removeAttribute(attrViewModel: AttributeInformationViewModel) {
        removedAttributes.add(attrViewModel)
        currentAttributeViewModelList.remove(attrViewModel)
        attributeViewModelListObservable.onNext(currentAttributeViewModelList)
    }

    private fun saveAttributes(trackerDao: OTTrackerDAO) {
        trackerDao.attributes.clear()
        currentAttributeViewModelList.forEachWithIndex { index, attrViewModel ->
            if (!attrViewModel.isInDatabase) {
                println("viewmodel ${attrViewModel.name} is not in database.")
                println("viewmodel attribute local id: ${trackerDao.attributeLocalKeySeed}")
                attrViewModel.attributeDAO.localId = trackerDao.attributeLocalKeySeed
                trackerDao.attributeLocalKeySeed++
                attrViewModel.attributeDAO.trackerId = trackerDao.objectId

                println("viewmodel attribute tracker id: ${trackerDao.objectId}")
                attrViewModel.saveToRealm()
            }
            attrViewModel.attributeDAO.position = index
            attrViewModel.applyChanges()
            trackerDao.attributes.add(attrViewModel.attributeDAO)
        }
        removedAttributes.forEach { viewModel ->
            if (viewModel.isInDatabase) {
                viewModel.attributeDAO.deleteFromRealm()
            }
        }
        removedAttributes.clear()
    }

    fun applyChanges(): String {
        if (trackerDao != null) {
            trackerDao?.let { dao ->
                realm.executeTransaction {
                    dao.name = nameObservable.value
                    dao.isBookmarked = isBookmarkedObservable.value
                    dao.color = colorObservable.value
                    saveAttributes(dao)
                }
            }

        } else {
            realm.executeTransaction {
                val trackerDao = realm.createObject(OTTrackerDAO::class.java, UUID.randomUUID().toString())
                trackerDao.userId = OTAuthManager.userId
                trackerDao.name = nameObservable.value
                trackerDao.isBookmarked = isBookmarkedObservable.value
                trackerDao.color = colorObservable.value
                saveAttributes(trackerDao)

                this.trackerDao = trackerDao
            }
        }

        currentAttributeViewModelList.forEach {
            it.register()
        }

        return trackerDao!!.objectId!!
    }

    val hasChanges: Boolean
        get() {
            return trackerDao?.let { dao ->
                dao.name != nameObservable.value ||
                        dao.color != colorObservable.value ||
                        dao.isBookmarked != isBookmarkedObservable.value
            } ?: true
        }

    private fun clearCurrentAttributeList() {
        currentAttributeViewModelList.forEach {
            it.unregister()
        }
        currentAttributeViewModelList.clear()
    }


    override fun onCleared() {
        super.onCleared()
        clearCurrentAttributeList()
        removedAttributes.clear()
    }

    class AttributeInformationViewModel(_attributeDAO: OTAttributeDAO, val realm: Realm) : RealmChangeListener<OTAttributeDAO> {
        var attributeDAO: OTAttributeDAO = _attributeDAO
            private set

        val isInDatabase: Boolean get() = attributeDAO.isManaged

        val nameObservable = BehaviorSubject.create<String>("")
        val typeObservable = BehaviorSubject.create<Int>(-1)
        val iconObservable = BehaviorSubject.create<Int>(R.drawable.icon_small_longtext)
        val connectionObservable = BehaviorSubject.create<Nullable<OTConnection>>()
        val defaultValuePolicyObservable = BehaviorSubject.create<Int>(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL)
        val defaultValuePresetObservable = BehaviorSubject.create<Nullable<String>>(Nullable<String>(null))

        val onPropertyChanged = PublishSubject.create<Long>()

        val internalSubscription = CompositeSubscription()

        //key, dbId/serializedValue
        private val propertyTable = Hashtable<String, Pair<String, String>>()

        var name: String
            get() = nameObservable.value
            set(value) {
                if (nameObservable.value != value) {
                    nameObservable.onNext(value)
                }
            }

        var icon: Int
            get() = iconObservable.value
            set(@DrawableRes value) {
                if (iconObservable.value != value) {
                    iconObservable.onNext(value)
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

        init {
            if (isInDatabase)
                attributeDAO.addChangeListener(this)

            onChange(attributeDAO)

        }

        fun unregister() {
            if (isInDatabase)
                attributeDAO.removeChangeListener(this)

            internalSubscription.clear()
        }

        override fun onChange(snapshot: OTAttributeDAO) {
            if (snapshot.isValid) {
                applyDaoChangesToFront(snapshot)
            }
        }

        fun makeFrontalChangesToDao(): OTAttributeDAO {
            val dao = OTAttributeDAO()
            dao.objectId = attributeDAO.objectId
            dao.type = attributeDAO.type
            dao.localId = attributeDAO.localId
            dao.position = attributeDAO.position
            dao.trackerId = attributeDAO.trackerId
            dao.updatedAt = attributeDAO.updatedAt
            dao.isRequired = attributeDAO.isRequired

            dao.fallbackValuePolicy = this.defaultValuePolicy
            dao.fallbackPresetSerializedValue = this.defaultValuePreset
            dao.name = name
            for (entry in propertyTable) {
                dao.setPropertySerializedValue(entry.key, entry.value.second)
            }

            dao.serializedConnection = connectionObservable.value?.datum?.getSerializedString()

            return dao
        }

        fun applyDaoChangesToFront(editedDao: OTAttributeDAO) {
            name = editedDao.name

            defaultValuePolicy = editedDao.fallbackValuePolicy
            defaultValuePreset = editedDao.fallbackPresetSerializedValue

            println("serialized connection of dao: ${editedDao.serializedConnection}")
            editedDao.serializedConnection?.let {
                val connection = OTConnection.fromJson(it)
                if (connectionObservable.value?.datum != connection) {
                    connectionObservable.onNext(Nullable(connection))
                }
            } ?: if (connectionObservable.value?.datum != null) {
                connectionObservable.onNext(Nullable(null))
            }

            propertyTable.clear()
            var changed = false
            editedDao.properties.forEach { entry ->
                if (propertyTable[entry.key] != Pair(entry.id, entry.value!!)) {
                    propertyTable[entry.key] = Pair(entry.id, entry.value!!)
                    changed = true
                }
            }

            val helper = OTAttributeManager.getAttributeHelper(editedDao.type)
            icon = helper.getTypeSmallIconResourceId(editedDao)
            if (changed) {
                onPropertyChanged.onNext(System.currentTimeMillis())
            }

            if (typeObservable.value != editedDao.type) {
                typeObservable.onNext(editedDao.type)
            }
        }

        fun applyChanges() {
            attributeDAO.name = nameObservable.value
            attributeDAO.updatedAt = System.currentTimeMillis()
            attributeDAO.fallbackValuePolicy = defaultValuePolicy
            for (entry in propertyTable) {
                println("set new serialized value: $entry")
                attributeDAO.setPropertySerializedValue(entry.key, entry.value.second)
                println("get serialized value: ${attributeDAO.getPropertySerializedValue(entry.key)}")
            }
            attributeDAO.serializedConnection = connectionObservable.value?.datum?.getSerializedString()
        }

        fun saveToRealm() {
            this.attributeDAO = realm.copyToRealm(this.attributeDAO)
        }

        fun register() {
            if (isInDatabase)
                attributeDAO.addChangeListener(this)
        }
    }

    class AttributeViewModelListDiffUtilCallback(val oldList: List<AttributeInformationViewModel>, val newList: List<AttributeInformationViewModel>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition] ||
                    oldList[oldItemPosition].attributeDAO.objectId == newList[newItemPosition].attributeDAO.objectId
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }


    }
}