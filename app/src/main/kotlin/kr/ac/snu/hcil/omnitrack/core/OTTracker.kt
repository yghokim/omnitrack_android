package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.content.Context
import android.graphics.Color
import com.google.firebase.database.DatabaseReference
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AttributeSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
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
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId: String?, name: String, color: Int = Color.WHITE, isOnShortcut: Boolean = false, attributeIdSeed: Long = 0, _attributes: Collection<OTAttribute<out Any>>? = null)
    : NamedObject(objectId, name) {
    override val databasePointRef: DatabaseReference?
        get() = FirebaseHelper.dbRef?.child(FirebaseHelper.CHILD_NAME_TRACKERS)?.child(objectId)

    override fun makeNewObjectId(): String {
        return FirebaseHelper.generateNewKey(FirebaseHelper.CHILD_NAME_TRACKERS)
    }

    val attributes = ObservableList<OTAttribute<out Any>>()

    private val _removedAttributeIds = ArrayList<Long>()
    fun fetchRemovedAttributeIds(): LongArray {
        val result = _removedAttributeIds.toLongArray()
        _removedAttributeIds.clear()
        return result;
    }

    var owner: OTUser? by Delegates.observable(null as OTUser?){
        prop, old, new ->
        if (old != null) {
            removedFromUser.invoke(this, old)
        }
        if (new != null) {
            if (!suspendDatabaseSync)
                databasePointRef?.child("user")?.setValue(new.objectId)
            addedToUser.invoke(this, new)
        }
    }


    var attributeIdSeed: Long = attributeIdSeed
        private set(value) {
            if (field != value) {
                field = value
                if (!suspendDatabaseSync)
                    databasePointRef?.child("attribute_id_seed")?.setValue(value)
            }
        }

    var color: Int by Delegates.observable(color)
    {
        prop, old, new ->
        if (old != new) {
            colorChanged.invoke(this, new)
            OTShortcutPanelManager.notifyAppearanceChanged(this)
            colorSubject.onNext(new)

            if (!suspendDatabaseSync)
                databasePointRef?.child("color")?.setValue(new)
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

            databasePointRef?.child("is_on_shortcut")?.setValue(new)
        }
    }

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    val attributeAdded = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()
    val attributeRemoved = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()

    val colorChanged = Event<Int>()

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
    }

    override fun onNameChanged(newName: String) {
        super.onNameChanged(newName)
        OTShortcutPanelManager.notifyAppearanceChanged(this)
    }

    private fun onAttributeAdded(new: OTAttribute<out Any>, index: Int) {
        new.tracker = this
        /*
        if (new.dbId != null)
            _removedAttributeIds.removeAll { it == new.dbId }*/

        if (!suspendDatabaseSync)
            FirebaseHelper.saveAttribute(this.objectId, new, index)

        attributeAdded.invoke(this, ReadOnlyPair(new, index))
    }

    private fun onAttributeRemoved(attribute: OTAttribute<out Any>, index: Int) {
        attribute.tracker = null

        /*
        if (attribute.dbId != null)
            _removedAttributeIds.add(attribute.dbId as Long)*/

        attributeRemoved.invoke(this, ReadOnlyPair(attribute, index))
        if (!suspendDatabaseSync) {
            FirebaseHelper.removeAttribute(objectId, attribute.objectId)
        }
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

    fun getNewAttributeObjectId(): Long {
        return ++attributeIdSeed
    }

}