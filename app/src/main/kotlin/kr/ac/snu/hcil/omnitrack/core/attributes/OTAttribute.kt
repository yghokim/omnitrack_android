package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.OTConnection
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedIntegerKeyEntry
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class OTAttribute<DataType>(objectId: String?, dbId: Long?, columnName: String, val typeId: Int, propertyData: String?, connectionData: String?) : NamedObject(objectId, dbId, columnName) {

    override fun makeNewObjectId(): String {
        return owner?.owner?.makeNewObjectId() ?: UUID.randomUUID().toString()
    }

    companion object {

        const val VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE = 0
        const val VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE = 1


        const val TYPE_NUMBER = 0
        const val TYPE_TIME = 1
        const val TYPE_TIMESPAN = 2
        const val TYPE_SHORT_TEXT = 3
        const val TYPE_LONG_TEXT = 4
        const val TYPE_LOCATION = 5
        const val TYPE_CHOICE = 6
        const val TYPE_RATING = 7
        const val TYPE_IMAGE = 8


        fun createAttribute(objectId: String?, dbId: Long?, columnName: String, typeId: Int, propertyData: String?, connectionData: String?): OTAttribute<out Any> {
            val attr = when (typeId) {
                TYPE_NUMBER -> OTNumberAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_TIME -> OTTimeAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_TIMESPAN -> OTTimeSpanAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_SHORT_TEXT -> OTShortTextAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_LONG_TEXT -> OTLongTextAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_LOCATION -> OTLocationAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_CHOICE -> OTChoiceAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_RATING -> OTRatingAttribute(objectId, dbId, columnName, propertyData, connectionData)
                TYPE_IMAGE -> OTImageAttribute(objectId, dbId, columnName, propertyData, connectionData)
                else -> OTNumberAttribute(objectId, dbId, columnName, propertyData, connectionData)
            }
            return attr
        }

        fun createAttribute(user: OTUser, columnName: String, typeId: Int): OTAttribute<out Any> {
            return createAttribute(user.getNewAttributeObjectId().toString(), null, columnName, typeId, null, null)
        }
    }

    val removedFromTracker = Event<OTTracker>()
    val addedToTracker = Event<OTTracker>()

    abstract val valueNumericCharacteristics: NumericCharacteristics

    abstract val propertyKeys: IntArray

    abstract val typeNameResourceId: Int

    abstract val typeSmallIconResourceId: Int

    val propertyValueChanged = Event<OTProperty.PropertyChangedEventArgs<out Any>>()
    private val settingsProperties = SparseArray<OTProperty<out Any>>()

    var valueConnection: OTConnection? by Delegates.observable(null as OTConnection?) {
        prop, old, new ->
        if (old != new) {
            isDirtySinceLastSync = true
        }
    }

    constructor(columnName: String, typeId: Int) : this(null, null, columnName, typeId, null, null)

    init {
        createProperties()
        if (propertyData != null) {

            val parser = Gson()
            val s = parser.fromJson(propertyData, Array<String>::class.java).map { parser.fromJson(it, SerializedIntegerKeyEntry::class.java) }

            for (entry in s) {
                setPropertyValueFromSerializedString(entry.key, entry.value)
            }
        }

        if (connectionData != null) {
            valueConnection = OTConnection(connectionData)
        }

        isDirtySinceLastSync = true
    }

    protected abstract fun createProperties()

    open fun compareValues(a: Any, b: Any): Int {
        return 0
    }

    fun getSerializedProperties(): String {
        val s = ArrayList<String>()
        val parser = Gson()
        for (key in propertyKeys) {
            s.add(parser.toJson(SerializedIntegerKeyEntry(key, getProperty<Any>(key).getSerializedValue())))
        }

        return parser.toJson(s.toTypedArray())
    }

    abstract val typeNameForSerialization: String

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
        isDirtySinceLastSync = true
    }

    fun <T> getProperty(key: Int): OTProperty<T> {
        @Suppress("UNCHECKED_CAST")
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

    abstract fun formatAttributeValue(value: Any): String

    /***
     * Autocompleted values based on attribute-specific settings.
     * [return] whether the method finished synchronously.
     */
    abstract fun getAutoCompleteValueAsync(resultHandler: (result: DataType) -> Unit): Boolean

    abstract fun getInputViewType(previewMode: Boolean = false): Int

    open fun makePropertyViews(context: Context): Collection<ReadOnlyPair<Int?, View>> {
        val result = ArrayList<ReadOnlyPair<Int?, View>>()
        for (key in propertyKeys) {
            result.add(ReadOnlyPair(key, getProperty<Any>(key).buildView(context)))
        }
        return result
    }

    //reuse recycled view if possible.
    open fun getInputView(context: Context, previewMode: Boolean, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == getInputViewType(previewMode))) {
                    recycledView!!
                } else {
                    AAttributeInputView.makeInstance(getInputViewType(previewMode), context)
                }

        refreshInputViewUI(view)
        view.previewMode = previewMode
        return view
    }

    abstract fun refreshInputViewUI(inputView: AAttributeInputView<out Any>);

    open fun getViewForItemListContainerType(): Int {
        return VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE
    }

    open fun getViewForItemList(context: Context, recycledView: View?): View {

        val target: TextView = if (recycledView is TextView) {
            recycledView
        } else TextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.viewForItemListTextAppearance)

        target.background = null

        return target
    }

    open fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is TextView) {
            if (value != null) {
                view.text = TextHelper.stringWithFallback(formatAttributeValue(value), "-")
            } else {
                view.text = "No value"
            }
            return true
        } else return false
    }

    open fun getRecommendedChartModels(): Array<ChartModel<*>> {
        return arrayOf()
    }
}