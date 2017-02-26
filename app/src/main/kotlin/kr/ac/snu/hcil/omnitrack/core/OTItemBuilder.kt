package kr.ac.snu.hcil.omnitrack.core

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
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 25
 */
class OTItemBuilder : IStringSerializable {

    class OTItemBuilderTypeAdapter : TypeAdapter<OTItemBuilder>() {

        override fun read(input: JsonReader): OTItemBuilder {
            input.beginObject()

            input.nextName()
            val trackerObjId = input.nextString()

            input.nextName()
            val itemId = input.nextString()

            input.nextName()
            val itemTimestamp = input.nextLong()

            input.nextName()
            val mode = input.nextInt()

            val builder = OTItemBuilder(trackerObjId, mode, if (itemId.isBlank()) {
                null
            } else {
                itemId
            }, if (itemTimestamp == -1L) {
                null
            } else {
                itemTimestamp
            })

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
            out.name("it").value(builder.connectedItemDbId ?: "")
            out.name("ts").value(builder.connectedItemTimestamp ?: -1L)
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

        /*
        @JvmField val CREATOR: Parcelable.Creator<OTItemBuilder> = object : Parcelable.Creator<OTItemBuilder> {
            override fun createFromParcel(source: Parcel): OTItemBuilder {
                return OTItemBuilder(source)
            }

            override fun newArray(size: Int): Array<OTItemBuilder?> {
                return arrayOfNulls(size)
            }
        }*/

        private val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTItemBuilder::class.java, OTItemBuilderTypeAdapter()).create()
        }

        fun getDeserializedInstanceWithTracker(jsonString: String, tracker: OTTracker): OTItemBuilder? {
            val instance = parser.fromJson(jsonString, OTItemBuilder::class.java)
            if (instance.trackerObjectId == tracker.objectId) {
                instance.setTracker(tracker)
                instance.syncFromTrackerScheme()
                return instance
            } else {
                return null
            }
        }

        fun getDeserializedInstance(jsonString: String): Observable<OTItemBuilder> {
            return OTApplication.app.currentUserObservable.map<OTItemBuilder>(
                    Func1<OTUser, OTItemBuilder> {
                        user ->
                        val instance = parser.fromJson(jsonString, OTItemBuilder::class.java)
                        instance.setTracker(user[instance.trackerObjectId]!!)
                        instance.syncFromTrackerScheme()
                        instance
                    }
            )
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

    private val trackerObjectId: String
    private lateinit var tracker: OTTracker

    private val connectedItemDbId: String?
    private val connectedItemTimestamp: Long?

    private val mode: Int

    val isEmpty: Boolean get() = valueTable.isEmpty

    /**
     * Used when editing item.
     * @param item: item should be already stored in DB. (Every item is immediately stored in DB when created.)
     */
    constructor(item: OTItem, tracker: OTTracker) {
        setTracker(tracker)
        trackerObjectId = tracker.objectId
        this.mode = MODE_EDIT
        connectedItemDbId = item.objectId
        connectedItemTimestamp = item.timestamp
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
        trackerObjectId = tracker.objectId

        this.mode = mode
        connectedItemDbId = null
        connectedItemTimestamp = null
        syncFromTrackerScheme()
    }


    /**
     * used when deserializing
     */
    /*
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
    }*/

    /*
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
    }*/

    private constructor(trackerObjectId: String, mode: Int, connectedItemDbId: String?, connectedItemTimestamp: Long?) {
        if (!OTApplication.app.isUserLoaded) {
            throw Exception("Do not deserialize ItemBuilder until user is loaded.")
        }
        this.mode = mode
        this.trackerObjectId = trackerObjectId
        this.connectedItemDbId = connectedItemDbId
        this.connectedItemTimestamp = connectedItemTimestamp
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

    /*
    override fun describeContents(): Int {
        return 0
    }*/

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

    fun autoComplete(onAttributeStateChangedListener: AttributeStateChangedListener? = null): Observable<Pair<Int, Any>> {

        return Observable.merge(tracker.attributes.unObservedList.mapIndexed { i, attr ->
            if (attr.valueConnection != null) {
                attr.valueConnection!!.getRequestedValue(this).flatMap { data ->
                        if (data.datum == null) {
                            attr.getAutoCompleteValue()
                        } else {
                            Observable.just(data.datum)
                        }
                }.onErrorResumeNext { attr.getAutoCompleteValue() }.map { value -> Pair(i, value) }.subscribeOn(Schedulers.io()).doOnSubscribe {

                    println("RX doOnSubscribe1: ${Thread.currentThread().name}")
                    attributeStateList[i] = EAttributeValueState.GettingExternalValue
                    onAttributeStateChangedListener?.onAttributeStateChanged(attr, i, EAttributeValueState.GettingExternalValue)
                }
            } else {
                attr.getAutoCompleteValue().map {
                    data ->
                    Pair(i, data as Any)
                }.doOnSubscribe {
                    println("RX doOnSubscribe2: ${Thread.currentThread().name}")
                    attributeStateList[i] = EAttributeValueState.Processing
                    onAttributeStateChangedListener?.onAttributeStateChanged(attr, i, EAttributeValueState.Processing)
                }
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).doOnSubscribe {
            println("RX Subscribe to ITemBuilder Autocomplete: ${Thread.currentThread().name} ==========================================")
        }.doOnNext {
            result ->

            println("RX doOnNext: ${Thread.currentThread().name}")
            val index = result.first
            val value = result.second

            val attribute = tracker.attributes[index]
            attributeStateList[index] = EAttributeValueState.Idle

            println("attribute ${index} (${attribute.name}) was complete: ${value}")

            onAttributeStateChangedListener?.onAttributeStateChanged(attribute, index, EAttributeValueState.Idle)
            setValueOf(attribute, value)
        }.doOnCompleted {
            println("RX finished autocompleting builder=======================")
        }
    }

    private fun setTracker(tracker: OTTracker) {
        this.tracker = tracker

        attributeStateList.clear()
        for (i in 1..tracker.attributes.size) {
            attributeStateList.add(EAttributeValueState.Idle)
        }
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
        val item = OTItem(tracker, connectedItemTimestamp ?: -1L, source, *(tracker.attributes.filter { hasValueOf(it) }.map { getValueInformationOf(it)!!.value }.toTypedArray()))

        if (connectedItemDbId != null) {
            println("assigned db id : $connectedItemDbId")
            item.objectId = connectedItemDbId
        }

        return item
    }
}