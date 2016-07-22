package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.util.SparseArray
import android.view.View
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
open abstract class OTAttribute<DataType>(objectId: String?, dbId: Long?, columnName: String, val typeId: Int, settingData: String?) : UniqueObject(objectId, dbId, columnName) {
    override fun makeNewObjectId(): String {
        return owner?.owner?.makeNewObjectId() ?: UUID.randomUUID().toString()
    }

    data class SerializedEntry(val key: Int, val value: String)

    companion object {

        const val TYPE_NUMBER = 0
        const val TYPE_TIME = 1
        const val TYPE_TIMESPAN = 2
        const val TYPE_LOCATION = 3

        fun createAttribute(objectId: String?, dbId: Long?, columnName: String, typeId: Int, settingData: String?): OTAttribute<out Any> {
            val attr = when (typeId) {
                TYPE_NUMBER -> OTNumberAttribute(objectId, dbId, columnName, settingData)
                TYPE_TIME -> OTTimeAttribute(objectId, dbId, columnName, settingData)
                else -> OTNumberAttribute(objectId, dbId, columnName, settingData)
            }
            return attr
        }

        fun createAttribute(user: OTUser, columnName: String, typeId: Int): OTAttribute<out Any> {
            return createAttribute(user.getNewAttributeObjectId().toString(), null, columnName, typeId, null)
        }


    }

    val removedFromTracker = Event<OTTracker>()
    val addedToTracker = Event<OTTracker>()

    abstract val keys: Array<Int>

    abstract val typeNameResourceId: Int
        get

    val propertyValueChanged = Event<OTProperty.PropertyChangedEventArgs<out Any>>()
    private val settingsProperties = SparseArray<OTProperty<out Any>>()

    constructor(columnName: String, typeId: Int) : this(null, null, columnName, typeId, null)

    init {
        createProperties()
        if (settingData != null) {

            val parser = Gson()
            val s = parser.fromJson(settingData, Array<String>::class.java).map { parser.fromJson(it, SerializedEntry::class.java) }

            for (entry in s) {
                setPropertyValueFromSerializedString(entry.key, entry.value)
            }
        }
    }

    protected abstract fun createProperties()

    fun getSerializedProperties(): String {
        val s = ArrayList<String>()
        val parser = Gson()
        for (key in keys) {
            s.add(parser.toJson(SerializedEntry(key, getProperty<Any>(key).getSerializedValue())))
        }

        return parser.toJson(s.toTypedArray())
    }

    var owner: OTTracker? by Delegates.observable(null as OTTracker?) {
        prop, old, new ->
        if (old != null) {
            removedFromTracker.invoke(this, old)
        }
        if (new != null) {
            addedToTracker.invoke(this, new)
        }
    }

    protected fun assignProperty(property: OTProperty<out Any>) {
        property.onValueChanged += {
            sender, args ->
            onPropertyValueChanged(args)
        }

        settingsProperties.put(property.key, property)
    }

    protected open fun onPropertyValueChanged(args: OTProperty.PropertyChangedEventArgs<out Any>) {
        propertyValueChanged.invoke(this, args)
    }

    fun <T> getProperty(key: Int): OTProperty<T> {
        return settingsProperties[key]!! as OTProperty<T>
    }

    fun <T> getPropertyValue(key: Int): T {
        return getProperty<T>(key).value
    }

    fun setPropertyValue(key: Int, value: Any) {
        getProperty<Any>(key).value = value
    }

    fun setPropertyValueFromSerializedString(key: Int, serializedValue: String) {
        getProperty<Any>(key).setValueFromSerializedString(serializedValue)
    }

    abstract fun parseAttributeValue(storedValue: String): DataType

    abstract fun formatAttributeValue(value: Any): String

    abstract fun makeDefaultValue(): DataType

    open fun makePropertyViews(context: Context): Collection<Pair<Int?, View>> {
        val result = ArrayList<Pair<Int?, View>>()
        for (key in keys) {
            result.add(Pair(key, getProperty<Any>(key).buildView(context)))
        }
        return result
    }

    //reuse recycled view if possible.
    abstract fun getInputView(context: Context, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any>
}