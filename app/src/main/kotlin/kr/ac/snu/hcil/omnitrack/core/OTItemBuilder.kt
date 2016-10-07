package kr.ac.snu.hcil.omnitrack.core

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilder : Parcelable, IStringSerializable {

    companion object {
        const val MODE_EDIT = 2
        const val MODE_FOREGROUND = 1
        const val MODE_BACKGROUND = 0

        @JvmField val CREATOR: Parcelable.Creator<OTItemBuilder> = object : Parcelable.Creator<OTItemBuilder> {
            override fun createFromParcel(source: Parcel): OTItemBuilder {
                return OTItemBuilder(source)
            }

            override fun newArray(size: Int): Array<OTItemBuilder?> {
                return arrayOfNulls(size)
            }
        }
    }

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attribute: OTAttribute<*>, position: Int, state: EAttributeValueState)
    }

    data class ValueInfo(var value: Any, var timestamp: Long)

    internal data class SerializationPackage(val trackerObjectId: String, val mode: Int, val connectedItemDbId: Long, val serializedEntries: Array<String>)

    private val attributeStateList = ArrayList<EAttributeValueState>()

    protected val valueTable = Hashtable<String, ValueInfo>()

    private lateinit var tracker: OTTracker

    private val connectedItemDbId: Long

    private val mode: Int

    val isEmpty: Boolean get() = valueTable.isEmpty

    /**
     * Used when editing item.
     * @param item: item should be already stored in DB. (Every item is immediately stored in DB when created.)
     */
    constructor(item: OTItem, tracker: OTTracker) {
        setTracker(tracker)

        this.mode = MODE_EDIT
        connectedItemDbId = item.dbId!!
        syncFromTrackerScheme()

        for (attribute in tracker.attributes) {
            if(item.hasValueOf(attribute)) {
                setValueOf(attribute, item.getValueOf(attribute)!!)
            }
        }
    }


    /**
     * Used when new item input mode
     */
    constructor(tracker: OTTracker, mode: Int) {
        setTracker(tracker)

        this.mode = mode
        connectedItemDbId = -1
        syncFromTrackerScheme()
    }


    /**
     * used when deserializing
     */
    constructor(parcel: Parcel) {

        val gson = Gson()

        reloadTracker(parcel.readString())

        this.mode = parcel.readInt()
        this.connectedItemDbId = parcel.readLong()

        for (serializedEntry in parcel.createStringArray()) {
            val entry = gson.fromJson(serializedEntry, Array<String>::class.java)

            valueTable[entry[0]] = ValueInfo(
                    TypeStringSerializationHelper.deserialize(entry[1]),
                    entry[2].toLong()
            )

        }

        syncFromTrackerScheme()
    }

    override fun writeToParcel(parcel: Parcel, content: Int) {
        parcel.writeString(tracker.objectId)
        parcel.writeInt(mode)
        parcel.writeLong(connectedItemDbId)

        val gson = Gson()
        parcel.writeStringArray(
                valueTable.map {
                    gson.toJson(arrayOf(it.key,
                            TypeStringSerializationHelper.serialize(it.value), it.value.timestamp.toString()))
                }.toTypedArray()
        )
    }


    constructor(serialized: String) {

        val gson = Gson()

        val pack = gson.fromJson(serialized, SerializationPackage::class.java)

        reloadTracker(pack.trackerObjectId)

        this.mode = pack.mode
        this.connectedItemDbId = pack.connectedItemDbId

        for (serializedEntry in pack.serializedEntries) {
            val entry = gson.fromJson(serializedEntry, Array<String>::class.java)

            valueTable[entry[0]] = ValueInfo(
                    TypeStringSerializationHelper.deserialize(entry[1]),
                    entry[2].toLong()
            )

        }

        syncFromTrackerScheme()
    }

    override fun getSerializedString(): String {

        val gson = Gson()

        val pack = SerializationPackage(tracker.objectId, mode, connectedItemDbId,
                tracker.attributes.mapNonNull {
                    val valueInfo = valueTable[it.objectId]
                    if (valueInfo != null) {
                        gson.toJson(arrayOf(it.objectId,
                                TypeStringSerializationHelper.serialize(it.typeNameForSerialization, valueInfo.value), valueInfo.timestamp.toString()))
                    } else null
                }.toTypedArray()
        )

        val json = gson.toJson(pack)
        println(json)
        return json
    }

    override fun fromSerializedString(serialized: String): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getValueInformationOf(attribute: OTAttribute<out Any>): ValueInfo? {
        return valueTable[attribute.objectId]
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getCastedValueOf(attribute: OTAttribute<T>): T? {
        return valueTable[attribute.objectId]?.value as? T
    }

    fun setValueOf(attribute: OTAttribute<out Any>, value: Any, timestamp: Long = System.currentTimeMillis()) {
        synchronized(valueTable) {
            val info = valueTable[attribute.objectId]
            if (info != null) {
                info.value = value
                info.timestamp = timestamp
            } else {
                valueTable[attribute.objectId] = ValueInfo(value, timestamp)
            }
        }
    }

    fun removeValueOf(attribute: OTAttribute<out Any>) {
        valueTable.remove(attribute.objectId)
    }

    fun hasValueOf(attribute: OTAttribute<out Any>): Boolean {
        return valueTable.containsKey(attribute.objectId)
    }

    fun getNumStoredAttributes(): Int {
        return valueTable.keys.size
    }

    fun clear() {
        valueTable.clear()
    }

    fun getAttributeValueState(position: Int): EAttributeValueState {
        return attributeStateList[position]
    }


    fun autoCompleteAsync(onAttributeStateChangedListener: AttributeStateChangedListener? = null,
                          finished: (() -> Unit)? = null) {

        var remain = tracker.attributes.size
        if (remain == 0) {
            finished?.invoke()
            return
        }

        for (attributeEntry in tracker.attributes.unObservedList.withIndex()) {
            val attribute = attributeEntry.value

                if (attribute.valueConnection == null) {
                    attributeStateList[attributeEntry.index] = EAttributeValueState.Processing
                    val isSynchronous = attribute.getAutoCompleteValueAsync {
                        result ->
                        synchronized(remain)
                        {
                            remain--
                            setValueOf(attribute, result)

                            attributeStateList[attributeEntry.index] = EAttributeValueState.Idle
                            onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.Idle)

                            if (remain == 0) {
                                //finish
                                finished?.invoke()
                                println("finished autocompleting builder")
                            }
                        }
                    }

                    if (!isSynchronous) {
                        onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.Processing)
                    }

                } else {
                    println("request value connection")

                    attributeStateList[attributeEntry.index] = EAttributeValueState.GettingExternalValue
                    onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.GettingExternalValue)

                    attribute.valueConnection?.requestValueAsync(this) {
                        value: Any? ->
                        if (value != null) {
                            synchronized(remain) {
                                remain--
                                setValueOf(attribute, value)
                                attributeStateList[attributeEntry.index] = EAttributeValueState.Idle
                                onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.Idle)


                                if (remain == 0) {
                                    //finish
                                    finished?.invoke()
                                    println("finished autocompleting builder")
                                }
                            }
                        } else {
                            attribute.getAutoCompleteValueAsync {
                                result ->
                                synchronized(remain) {
                                    remain--
                                    setValueOf(attribute, result)
                                    attributeStateList[attributeEntry.index] = EAttributeValueState.Idle
                                    onAttributeStateChangedListener?.onAttributeStateChanged(attribute, attributeEntry.index, EAttributeValueState.Idle)

                                    if (remain == 0) {
                                        //finish
                                        finished?.invoke()
                                        println("finished autocompleting builder")
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun setTracker(tracker: OTTracker) {
        this.tracker = tracker

        attributeStateList.clear()
        for (i in 1..tracker.attributes.size) {
            attributeStateList.add(EAttributeValueState.Idle)
        }
    }

    fun reloadTracker(trackerObjectId: String) {
        setTracker(OTApplication.app.currentUser[trackerObjectId]!!)
        syncFromTrackerScheme()
    }

    fun syncFromTrackerScheme() {

        val it = valueTable.entries.iterator()

        while (it.hasNext()) {
            val entry = it.next()

            if (tracker.attributes.unObservedList.find { it.objectId == entry.key } == null) {
                it.remove()
            }
        }
    }

    fun makeItem(): OTItem {
        val item = OTItem(tracker.objectId)
        if (connectedItemDbId != -1L) {
            println("assigned db id : $connectedItemDbId")
            item.dbId = connectedItemDbId
        }

        for (attribute in tracker.attributes) {
            if (hasValueOf(attribute)) {
                item.setValueOf(attribute, getValueInformationOf(attribute)!!.value)
            }
        }

        return item
    }
}