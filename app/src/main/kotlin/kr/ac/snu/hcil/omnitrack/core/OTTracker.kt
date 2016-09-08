package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.content.Context
import android.graphics.Color
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AttributeSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.TimelineComparisonLineChartModel
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId: String?, dbId: Long?, name: String, color: Int = Color.WHITE, isOnShortcut: Boolean = false, _attributes: Collection<OTAttribute<out Any>>? = null)
: NamedObject(objectId, dbId, name) {

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
            addedToUser.invoke(this, new)
        }
    }

    var color: Int by Delegates.observable(color)
    {
        prop, old, new ->
        if (old != new) {
            colorChanged.invoke(this, new)
            OTShortcutManager.notifyAppearanceChanged(this)
            isDirtySinceLastSync = true
        }
    }

    var isOnShortcut: Boolean by Delegates.observable(isOnShortcut){
        prop, old, new->
        if(old!=new)
        {
            if(new==true)
            {
                OTShortcutManager += this
            }
            else{
                OTShortcutManager -= this
            }

            isDirtySinceLastSync = true
        }
    }

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    val attributeAdded = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()
    val attributeRemoved = Event<ReadOnlyPair<OTAttribute<out Any>, Int>>()

    val colorChanged = Event<Int>()

    constructor(): this(null, null, "New Tracker")

    constructor(name: String): this(null,null, name)

    init{

        if (_attributes != null) {
            for (attribute in _attributes) {
                attributes.unObservedList.add(attribute)

                attribute.addedToTracker.suspend = true
                attribute.owner = this
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
            for (i in range) {
                attributes[i].isDirtySinceLastSync = true
            }
        }

        isDirtySinceLastSync = true
    }

    override fun onNameChanged(newName: String) {
        super.onNameChanged(newName)
        OTShortcutManager.notifyAppearanceChanged(this)
    }

    private fun onAttributeAdded(new: OTAttribute<out Any>, index: Int) {
        new.owner = this
        if (new.dbId != null)
            _removedAttributeIds.removeAll { it == new.dbId }

        attributeAdded.invoke(this, ReadOnlyPair(new, index))
    }

    private fun onAttributeRemoved(attribute: OTAttribute<out Any>, index: Int) {
        attribute.owner = null

        if (attribute.dbId != null)
            _removedAttributeIds.add(attribute.dbId as Long)

        attributeRemoved.invoke(this, ReadOnlyPair(attribute, index))
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
        return DefaultNameGenerator.generateName("${typeName} ${context.resources.getString(R.string.msg_attribute)}", attributes.unObservedList.map { it.name })
    }

    fun getRecommendedChartModels(): Array<ChartModel<*>> {

        val list = ArrayList<ChartModel<*>>()


        //generate tracker-level charts

        //TODO add heatmap timeline

        //add line timeline if numeric variables exist
        val numberAttrs = attributes.filter { it is OTNumberAttribute }.map { it as OTNumberAttribute }
        if (numberAttrs.size > 0) {
            list.add(TimelineComparisonLineChartModel(numberAttrs, this))
        }


        for(attribute in attributes)
        {
            list.addAll(attribute.getRecommendedChartModels())
        }

        return list.toTypedArray()
    }

}