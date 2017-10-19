package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.content.Context
import android.graphics.Color
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId: String?, name: String, color: Int = Color.WHITE, isOnShortcut: Boolean = false, isEditable: Boolean = true, attributeIdSeed: Int = 0, _attributes: Collection<OTAttribute<out Any>>? = null, val creationFlags: Map<String, String>? = null, _intrinsicPosition: Int = 0)
    : NamedObject(objectId, name) {

    companion object {
        const val PROPERTY_COLOR = "color"
        const val PROPERTY_IS_ON_SHORTCUT = "onShortcut"
        const val PROPERTY_IS_EDITABLE = "editable"
        const val PROPERTY_POSITION = "position"
        const val PROPERTY_ATTRIBUTES = "attributes"

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

    override fun makeNewObjectId(): String {
        return "" //DatabaseManager.generateNewKey(DatabaseManager.CHILD_NAME_TRACKERS)
    }
/*
    private var currentDbReference: DatabaseReference? = null
    private val dbChangedListener: ChildEventListener

    private var currentAttributesDbReference: DatabaseReference? = null
    private val attributesDbChangedListener: ChildEventListener*/

    val attributes = ObservableList<OTAttribute<out Any>>()

    var isEditable: Boolean = isEditable
        private set

    var owner: OTUser? by Delegates.observable(null as OTUser?) {
        prop, old, new ->
        if (old != new) {
            if (old != null) {
                removedFromUser.invoke(this, old)
            }
            if (new != null) {
                if (!suspendDatabaseSync) {
                    if (!suspendDatabaseSync) save()
                }

                /*
                onReminderAddedSubscription.set(
                        new.triggerManager.triggerAdded.filter {
                            trigger ->
                            trigger.trackers.contains(this) && trigger.action == OTTrigger.ACTION_NOTIFICATION
                        }.subscribe {
                            trigger ->
                            reminderAdded.onNext(ReadOnlyPair(this, trigger))
                        }
                )

                onReminderRemovedSubscription.set(
                        new.triggerManager.triggerRemoved.filter {
                            trigger ->
                            trigger.trackers.contains(this) && trigger.action == OTTrigger.ACTION_NOTIFICATION
                        }.subscribe {
                            trigger ->
                            reminderRemoved.onNext(ReadOnlyPair(this, trigger))
                        }
                )*/

                addedToUser.invoke(this, new)
            } else {
                /*
                onReminderAddedSubscription.set(Subscriptions.empty())
                onReminderRemovedSubscription.set(Subscriptions.empty())
                */
            }
        }
    }


    var attributeLocalKeySeed: Int = attributeIdSeed
        private set(value) {
            if (field != value) {
                field = value
                if (!suspendDatabaseSync) save()
            }
        }

    var intrinsicPosition: Int by Delegates.observable(_intrinsicPosition)
    {
        prop, old, new ->
        if (old != new) {
            if (!suspendDatabaseSync) save()
        }
    }

    var color: Int by Delegates.observable(color)
    {
        prop, old, new ->
        if (old != new) {
            OTShortcutPanelManager.notifyAppearanceChanged(this)

            if (!suspendDatabaseSync) save()
        }
    }

    var isOnShortcut: Boolean by Delegates.observable(isOnShortcut) {
        prop, old, new ->
        if (old != new) {
            if (new == true) {
                OTShortcutPanelManager += this
            } else {
                OTShortcutPanelManager -= this
            }

            if (!suspendDatabaseSync) save()
        }
    }

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    val attributeAdded = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()
    val attributeRemoved = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()

    val isExternalFilesInvolved: Boolean get() = attributes.unObservedList.find { it.isExternalFile } != null

    constructor() : this(null, "New Tracker", isEditable = true)

    constructor(name: String) : this(null, name, isEditable = true)

    init {
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
            sender, args ->
            onAttributeAdded(args.first, args.second)
        }

        attributes.elementRemoved += {
            sender, args ->
            onAttributeRemoved(args.first, args.second)
        }

        attributes.elementReordered += {
            sender, range ->
            if (!suspendDatabaseSync) {
                for (i in range) {
                    OTApplication.app.databaseManager.saveAttribute(this.objectId, attributes[i], i)
                }
                OTApplication.app.databaseManager.saveTracker(this, intrinsicPosition)
            }
        }

        suspendDatabaseSync = false

/*
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
                    PROPERTY_IS_EDITABLE -> {
                        this@OTTracker.isEditable = if (remove) {
                            true
                        } else {
                            snapshot.value as Boolean
                        }
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

        currentAttributesDbReference = databasePointRef?.child(DatabaseManager.CHILD_NAME_ATTRIBUTES)

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
                    val pojo = snapshot.getValue(DatabaseManager.AttributePOJO::class.java)
                    if (pojo != null) {
                        attribute.suspendDatabaseSync = true

                        attribute.name = pojo.name ?: "Noname"
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
                        val pojo = snapshot.getValue(DatabaseManager.AttributePOJO::class.java)
                        if (pojo?.type != null) {
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
        currentAttributesDbReference?.addChildEventListener(attributesDbChangedListener)*/
    }

    override fun save() {
        OTApplication.app.databaseManager.saveTracker(this, intrinsicPosition)
    }

    fun dispose() {
        //currentDbReference?.removeEventListener(dbChangedListener)
        //currentAttributesDbReference?.removeEventListener(attributesDbChangedListener)

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

        if (!suspendDatabaseSync) {
            OTApplication.app.databaseManager.saveTracker(this, intrinsicPosition)
            OTApplication.app.databaseManager.saveAttribute(this.objectId, new, index)
        }

        attributeAdded.invoke(this, ReadOnlyPair(new, index))
    }

    private fun onAttributeRemoved(attribute: OTAttribute<out Any>, index: Int) {
        attribute.tracker = null

        /*
        if (attribute.objectId != null)
            _removedAttributeIds.add(attribute.objectId as Long)*/

        attributeRemoved.invoke(this, ReadOnlyPair(attribute, index))
        if (!suspendDatabaseSync) {
            OTApplication.app.databaseManager.saveTracker(this, intrinsicPosition)
            OTApplication.app.databaseManager.removeAttribute(objectId, attribute.objectId)
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
                //list.add(AFieldValueSorter(attribute))
            }
        }

        return list
    }

    fun generateNewAttributeName(typeName: String, context: Context): String {
        return DefaultNameGenerator.generateName(typeName, attributes.unObservedList.map { it.name }, true)
    }

    fun getRecommendedChartModels(): Array<ChartModel<*>> {

        val list = ArrayList<ChartModel<*>>()

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