package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.content.Context
import android.graphics.Color
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AttributeSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.LoggingHeatMapModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.TimelineComparisonLineChartModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId: String?, name: String, color: Int = Color.WHITE, isOnShortcut: Boolean = false, attributeIdSeed: Int = 0, _attributes: Collection<OTAttribute<out Any>>? = null, val creationFlags: Map<String, String>? = null)
    : NamedObject(objectId, name) {

    companion object {
        const val PROPERTY_COLOR = "color"
        const val PROPERTY_IS_ON_SHORTCUT = "onShortcut"
        const val PROPERTY_ATTRIBUTES = "attributes"

        val CREATION_FLAG_TUTORIAL: Map<String, String> by lazy {
            val result = HashMap<String, String>()
            result["source"] = "generated_example"
            result
        }
    }

    override val databasePointRef: DatabaseReference?
        get() = FirebaseDbHelper.dbRef?.child(FirebaseDbHelper.CHILD_NAME_TRACKERS)?.child(objectId)

    override fun makeNewObjectId(): String {
        return FirebaseDbHelper.generateNewKey(FirebaseDbHelper.CHILD_NAME_TRACKERS)
    }

    private var currentDbReference: DatabaseReference? = null
    private val dbChangedListener: ChildEventListener

    private var currentAttributesDbReference: DatabaseReference? = null
    private val attributesDbChangedListener: ChildEventListener

    val attributes = ObservableList<OTAttribute<out Any>>()

    var owner: OTUser? by Delegates.observable(null as OTUser?){
        prop, old, new ->
        if (old != new) {
            if (old != null) {
                if (!suspendDatabaseSync) {
                    FirebaseDbHelper.setContainsFlagOfUser(old.objectId, this.objectId, FirebaseDbHelper.CHILD_NAME_TRACKERS, false)
                }

                removedFromUser.invoke(this, old)
            }
            if (new != null) {
                if (!suspendDatabaseSync) {
                    databasePointRef?.child("user")?.setValue(new.objectId)
                    FirebaseDbHelper.setContainsFlagOfUser(new.objectId, this.objectId, FirebaseDbHelper.CHILD_NAME_TRACKERS, true)
                }
                addedToUser.invoke(this, new)
            }
        }
    }


    var attributeLocalKeySeed: Int = attributeIdSeed
        private set(value) {
            if (field != value) {
                field = value
                if (!suspendDatabaseSync)
                    databasePointRef?.child("attributeLocalKeySeed")?.setValue(value)
            }
        }

    var color: Int by Delegates.observable(color)
    {
        prop, old, new ->
        if (old != new) {
            colorChanged.onNext(ReadOnlyPair(this, new))
            OTShortcutPanelManager.notifyAppearanceChanged(this)
            colorSubject.onNext(new)

            if (!suspendDatabaseSync)
                databasePointRef?.child(PROPERTY_COLOR)?.setValue(new)
        }
    }

    private val colorSubject = BehaviorSubject.create<Int>()
    val colorObservable: rx.Observable<Int> get() = colorSubject

    var isOnShortcut: Boolean by Delegates.observable(isOnShortcut){
        prop, old, new->
        if(old!=new)
        {
            if(new==true)
            {
                OTShortcutPanelManager += this
            }
            else{
                OTShortcutPanelManager -= this
            }

            if (!suspendDatabaseSync)
                databasePointRef?.child(PROPERTY_IS_ON_SHORTCUT)?.setValue(new)

            isOnShortcutChanged.onNext(ReadOnlyPair(this, new))
        }
    }

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    val attributeAdded = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()
    val attributeRemoved = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()

    val colorChanged = SerializedSubject(PublishSubject.create<ReadOnlyPair<OTTracker, Int>>())
    val isOnShortcutChanged = SerializedSubject(PublishSubject.create<ReadOnlyPair<OTTracker, Boolean>>())

    val isExternalFilesInvolved: Boolean get() = attributes.unObservedList.find { it.isExternalFile } != null

    constructor() : this(null, "New Tracker")

    constructor(name: String) : this(null, name)

    init{
        suspendDatabaseSync = true
        if (_attributes != null) {
            for (attribute in _attributes) {
                attributes.unObservedList.add(attribute)

                attribute.addedToTracker.suspend = true
                attribute.tracker = this
                attribute.addedToTracker.suspend = false

            }
        }

        attributes.elementAdded += {
            sender, args->
            onAttributeAdded(args.first, args.second)
        }

        attributes.elementRemoved += {
            sender, args->
            onAttributeRemoved(args.first, args.second)
        }

        attributes.elementReordered += {
            sender, range ->
            if (!suspendDatabaseSync) {
                for (i in range) {
                    attributes[i].databasePointRef?.child("position")?.setValue(i)
                }
            }
        }

        colorSubject.onNext(color)

        isDirtySinceLastSync = true
        suspendDatabaseSync = false


        currentDbReference = databasePointRef
        dbChangedListener = object : ChildEventListener {

            private fun handleChildChange(snapshot: DataSnapshot, remove: Boolean) {
                suspendDatabaseSync = true
                when (snapshot.key) {
                    PROPERTY_NAME ->
                        this@OTTracker.name = if (remove) {
                            "Noname"
                        } else {
                            snapshot.value.toString()
                        }

                    PROPERTY_IS_ON_SHORTCUT ->
                        if (remove) {

                            this@OTTracker.isOnShortcut = false
                        } else {
                            this@OTTracker.isOnShortcut = snapshot.value as Boolean
                        }
                    PROPERTY_COLOR ->
                        this@OTTracker.color = if (remove) {
                            Color.WHITE
                        } else {
                            val value = snapshot.value
                            (value as? Long)?.toInt() ?: (value as? Int ?: Color.WHITE)
                        }
                }
                suspendDatabaseSync = false
            }

            override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                println("tracker ${name} child added: ${snapshot.key}")
                handleChildChange(snapshot, false)
            }

            override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
                println("tracker ${name} child changed: ${snapshot.key}")
                handleChildChange(snapshot, false)
            }

            override fun onChildMoved(snapshot: DataSnapshot, p1: String?) {

                println("tracker ${name} child moved: ${snapshot.key}")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                println("tracker ${name} child removed: ${snapshot.key}")
                handleChildChange(snapshot, true)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }

        currentAttributesDbReference = databasePointRef?.child(FirebaseDbHelper.CHILD_NAME_ATTRIBUTES)

        println("attributes reference: ")
        println(currentAttributesDbReference)

        attributesDbChangedListener = object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                p0?.toException()?.printStackTrace()
            }

            override fun onChildMoved(snapshot: DataSnapshot, p1: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {
                suspendDatabaseSync = true
                val attribute = attributes.unObservedList.find { it.objectId == snapshot.key }
                if (attribute != null) {
                    val pojo = snapshot.getValue(FirebaseDbHelper.AttributePOJO::class.java)
                    if (pojo != null) {
                        attribute.suspendDatabaseSync = true

                        attribute.name = pojo.name ?: "Noname"
                        var changedValue = false
                        val properties = pojo.properties
                        if (properties != null) {
                            attribute.readPropertiesFromDatabase(properties)
                        }
                        attribute.suspendDatabaseSync = false
                    }
                }
                suspendDatabaseSync = false
            }

            override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
                synchronized(attributes.unObservedList)
                {
                    suspendDatabaseSync = true
                    val attrId = snapshot.key
                    if (attributes.unObservedList.find { it.objectId == attrId } == null) {
                        val pojo = snapshot.getValue(FirebaseDbHelper.AttributePOJO::class.java)
                        if (pojo != null && pojo.type != null) {
                            attributes.addAt(OTAttribute.Companion.createAttribute(snapshot.key, pojo), pojo.position)
                        }
                    }

                    suspendDatabaseSync = false
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                synchronized(attributes.unObservedList)
                {
                    suspendDatabaseSync = true
                    val attrId = snapshot.key

                    val attribute = attributes.unObservedList.find { it.objectId == attrId }
                    if (attribute != null) {
                        attributes.remove(attribute)
                    }
                    suspendDatabaseSync = false
                }
            }

        }

        currentDbReference?.addChildEventListener(dbChangedListener)
        currentAttributesDbReference?.addChildEventListener(attributesDbChangedListener)
    }

    fun dispose() {
        currentDbReference?.removeEventListener(dbChangedListener)
        currentAttributesDbReference?.removeEventListener(attributesDbChangedListener)
    }

    fun getItemCacheDir(context: Context, createIfNotExist: Boolean = true): File {
        val file = context.externalCacheDir.resolve("${owner?.objectId ?: "anonymous"}/${objectId}")
        if (createIfNotExist && !file.exists()) {
            file.mkdirs()
        }
        return file
    }

    fun getTotalCacheFileSize(context: Context): Long {
        val cacheDirectory = getItemCacheDir(context, false)
        try {
            if (cacheDirectory.isDirectory && cacheDirectory.exists()) {

                fun getSizeRecur(dir: File): Long {
                    var size = 0L

                    if (dir.isDirectory) {
                        for (file in dir.listFiles()) {
                            if (file.isFile) {
                                size += file.length()
                            } else {
                                size += getSizeRecur(file)
                            }
                        }
                    } else if (dir.isFile) {
                        size += dir.length()
                    }

                    return size
                }

                return getSizeRecur(cacheDirectory)
            } else {
                return 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    override fun onNameChanged(newName: String) {
        super.onNameChanged(newName)
        OTShortcutPanelManager.notifyAppearanceChanged(this)

        nameChanged.onNext(Pair(this, newName))
    }

    fun nextAttributeLocalKey(): Int {
        return ++attributeLocalKeySeed
    }

    private fun onAttributeAdded(new: OTAttribute<out Any>, index: Int) {
        new.tracker = this
        /*
        if (new.objectId != null)
            _removedAttributeIds.removeAll { it == new.objectId }*/

        if (!suspendDatabaseSync)
            FirebaseDbHelper.saveAttribute(this.objectId, new, index)

        attributeAdded.invoke(this, ReadOnlyPair(new, index))
    }

    private fun onAttributeRemoved(attribute: OTAttribute<out Any>, index: Int) {
        attribute.tracker = null

        /*
        if (attribute.objectId != null)
            _removedAttributeIds.add(attribute.objectId as Long)*/

        attributeRemoved.invoke(this, ReadOnlyPair(attribute, index))
        if (!suspendDatabaseSync) {
            FirebaseDbHelper.removeAttribute(objectId, attribute.objectId)
        }
    }

    fun getRequiredPermissions(): Array<String> {
        val list = ArrayList<String>()
        attributes.unObservedList.forEach {
            val perms = it.requiredPermissions()
            if (perms != null) {
                list.addAll(perms)
            }
        }

        return list.toTypedArray()
    }

    fun getSupportedComparators(): List<ItemComparator> {
        val list = ArrayList<ItemComparator>()
        list.add(ItemComparator.TIMESTAMP_SORTER)

        for (attribute in attributes) {
            if (attribute.valueNumericCharacteristics.sortable) {
                list.add(AttributeSorter(attribute))
            }
        }

        return list
    }

    fun generateNewAttributeName(typeName: String, context: Context): String {
        return DefaultNameGenerator.generateName(typeName, attributes.unObservedList.map { it.name }, true)
    }

    fun getRecommendedChartModels(): Array<ChartModel<*>> {

        val list = ArrayList<ChartModel<*>>()


        //generate tracker-level charts

        list.add(
                LoggingHeatMapModel(this)
        )

        //add line timeline if numeric variables exist
        val numberAttrs = attributes.filter { it is OTNumberAttribute }.map { it as OTNumberAttribute }
        if (numberAttrs.isNotEmpty()) {
            list.add(TimelineComparisonLineChartModel(numberAttrs, this))
        }


        for(attribute in attributes)
        {
            list.addAll(attribute.getRecommendedChartModels())
        }

        return list.toTypedArray()
    }

    fun isMeasureFactoryConnected(measureFactory: OTMeasureFactory): Boolean {
        for (attr in attributes) {
            if (attr.isMeasureFactoryConnected(measureFactory)) return true
        }

        return false
    }

    fun isValid(invalidMessages: MutableList<CharSequence>?): Boolean {

        var invalid = false

        for (attr in attributes) {
            if (!attr.isConnectionValid(invalidMessages)) {
                invalid = true
            }
        }

        return !invalid
    }
}