package kr.ac.snu.hcil.omnitrack.core

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.serialization.IStringSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilder : Parcelable, IStringSerializable {

    internal data class ValueParcel(val key: String, val serializedValue: String, val timeStamp: Long)

    internal class ValueParcelTypeAdapter : TypeAdapter<ValueParcel>() {

        override fun read(input: JsonReader): ValueParcel {
            input.beginObject()
            input.nextName()
            val k = input.nextString()
            input.nextName()
            val s = input.nextString()
            input.nextName()
            val t = input.nextLong()
            input.endObject()

            return ValueParcel(k, s, t)
        }

        override fun write(out: JsonWriter, value: ValueParcel) {
            out.beginObject()
            out.name("k").value(value.key)
            out.name("s").value(value.serializedValue)
            out.name("t").value(value.timeStamp)
            out.endObject()
        }
    }

    class OTItemBuilderTypeAdapter : TypeAdapter<OTItemBuilder>() {

        override fun read(input: JsonReader): OTItemBuilder {
            input.beginObject()
            input.nextName()
            val trackerObjId = input.nextString()
            input.nextName()
            val itemId = input.nextLong()
            input.nextName()
            val mode = input.nextInt()

            val builder = OTItemBuilder(trackerObjId, mode, itemId)

            input.nextName()
            input.beginArray()

            while (input.hasNext()) {
                input.beginObject()

                input.nextName()
                val key = input.nextString()

                input.nextName()
                val value = input.nextString()

                input.nextName()
                val timeStamp = input.nextLong()

                builder.valueTable[key] = ValueInfo(TypeStringSerializationHelper.deserialize(value), timeStamp)
                input.endObject()
            }

            input.endArray()

            input.endObject()

            return builder
        }

        override fun write(out: JsonWriter, builder: OTItemBuilder) {
            out.beginObject()
            out.name("tr").value(builder.tracker.objectId)
            out.name("it").value(builder.connectedItemDbId)
            out.name("md").value(builder.mode)
            out.name("v").beginArray()

            builder.tracker.attributes.unObservedList.forEach {
                val valueInfo = builder.valueTable[it.objectId]
                if (valueInfo != null) {
                    out.beginObject()
                    out.name("k").value(it.objectId)
                    out.name("v").value(TypeStringSerializationHelper.serialize(it.typeNameForSerialization, valueInfo.value))
                    out.name("t").value(valueInfo.timestamp)
                    out.endObject()
                }
            }
            out.endArray()

            out.endObject()

        }

    }


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

        val parcelParser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(ValueParcel::class.java, ValueParcelTypeAdapter()).create()
        }

        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTItemBuilder::class.java, OTItemBuilderTypeAdapter()).create()
        }
    }

    enum class EAttributeValueState {
        Processing, GettingExternalValue, Idle
    }

    interface AttributeStateChangedListener {
        fun onAttributeStateChanged(attribute: OTAttribute<*>, position: Int, state: EAttributeValueState)
    }

    data class ValueInfo(var value: Any, var timestamp: Long)

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
            if (item.hasValueOf(attribute)) {
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

        reloadTracker(parcel.readString())

        this.mode = parcel.readInt()
        this.connectedItemDbId = parcel.readLong()

        val parcelArray = parcelParser.fromJson(parcel.readString(), Array<ValueParcel>::class.java)

        for (entry in parcelArray) {
            valueTable[entry.key] = ValueInfo(
                    TypeStringSerializationHelper.deserialize(entry.serializedValue),
                    entry.timeStamp
            )

        }

        syncFromTrackerScheme()
    }

    override fun writeToParcel(parcel: Parcel, content: Int) {
        parcel.writeString(tracker.objectId)
        parcel.writeInt(mode)
        parcel.writeLong(connectedItemDbId)


        parcel.writeString(
                parcelParser.toJson(valueTable.map {
                    ValueParcel(it.key,
                            TypeStringSerializationHelper.serialize(it.value), it.value.timestamp)
                }.toTypedArray()
                ))
    }

    constructor(trackerObjectId: String, mode: Int, connectedItemDbId: Long) {
        reloadTracker(trackerObjectId)
        this.mode = mode
        this.connectedItemDbId = connectedItemDbId
        syncFromTrackerScheme()
    }
/*
    constructor(serialized: String) {

        val gson = Gson()

        val pack = gson.fromJson(serialized, SerializationPackage::class.java)


        reloadTracker(pack.trackerObjectId)

        this.mode = pack.mode
        this.connectedItemDbId = pack.connectedItemDbId

        val parcelArray = parcelParser.fromJson(pack.valueParcelArray, Array<ValueParcel>::class.java)

        for (entry in parcelArray) {
            valueTable[entry.key] = ValueInfo(
                    TypeStringSerializationHelper.deserialize(entry.serializedValue),
                    entry.timeStamp
            )

        }

        syncFromTrackerScheme()
    }*/

    override fun getSerializedString(): String {
        return parser.toJson(this)
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

    fun autoComplete(onAttributeStateChangedListener: AttributeStateChangedListener? = null, finished: (() -> Unit)? = null) {
        Observable.merge(tracker.attributes.unObservedList.mapIndexed { i, attr ->
            if (attr.valueConnection != null) {
                attributeStateList[i] = EAttributeValueState.GettingExternalValue
                onAttributeStateChangedListener?.onAttributeStateChanged(attr, i, EAttributeValueState.GettingExternalValue)
                attr.valueConnection!!.getRequestedValue(this).map {
                    data ->
                    Pair(i, data as Any)
                }
            } else {
                attributeStateList[i] = EAttributeValueState.Processing
                onAttributeStateChangedListener?.onAttributeStateChanged(attr, i, EAttributeValueState.Processing)
                attr.getAutoCompleteValue().map {
                    data ->
                    Pair(i, data as Any)
                }
            }
        })
                .subscribe(
                        {
                            result ->
                            val index = result.first
                            val value = result.second
                            val attribute = tracker.attributes[index]
                            attributeStateList[index] = EAttributeValueState.Idle
                            onAttributeStateChangedListener?.onAttributeStateChanged(attribute, index, EAttributeValueState.Idle)
                            setValueOf(attribute, value)
                        },
                        {

                        },
                        {
                            finished?.invoke()
                            println("finished autocompleting builder")
                        }
                )
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

    fun makeItem(source: OTItem.LoggingSource): OTItem {
        val item = OTItem(tracker.objectId, source)
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