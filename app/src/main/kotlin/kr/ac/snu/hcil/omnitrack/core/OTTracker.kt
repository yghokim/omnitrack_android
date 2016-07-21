package kr.ac.snu.hcil.omnitrack.core

//import kr.ac.snu.hcil.omnitrack.core.database.TrackerEntity
import android.graphics.Color
import kr.ac.snu.hcil.omnitrack.utils.ObservableList
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
class OTTracker(objectId: String?, dbId: Long?, name: String, color: Int = Color.WHITE, _attributes: Collection<OTAttribute<out Any>>? = null) : UniqueObject(objectId, dbId, name) {
    val attributes = ObservableList<OTAttribute<out Any>>()

    private val _removedAttributeIds = ArrayList<Long>()
    fun fetchRemovedAttributeIds(): Array<Long> {
        val result = _removedAttributeIds.toTypedArray()
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

    var color: Int = color

    val removedFromUser = Event<OTUser>()
    val addedToUser = Event<OTUser>()

    val attributeAdded = Event<Pair<OTAttribute<out Any>, Int>>()
    val attributeRemoved = Event<Pair<OTAttribute<out Any>, Int>>()

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
    }

    private fun onAttributeAdded(new: OTAttribute<out Any>, index: Int) {
        new.owner = this
        _removedAttributeIds.remove(new.dbId)
        attributeAdded.invoke(this, Pair(new, index))
    }

    private fun onAttributeRemoved(attribute: OTAttribute<out Any>, index: Int) {
        attribute.owner = null

        if (attribute.dbId != null)
            _removedAttributeIds.add(attribute.dbId as Long)

        attributeRemoved.invoke(this, Pair(attribute, index))
    }


}