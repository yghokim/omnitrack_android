package kr.ac.snu.hcil.omnitrack.core.attributes

import android.Manifest
import android.content.Context
import android.util.SparseArray
import android.view.View
import android.widget.TextView
import com.google.firebase.database.DatabaseReference
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedIntegerKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.integerKeyEntryParser
import rx.Observable
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class OTAttribute<DataType>(objectId: String?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, val typeId: Int, propertyData: String?, connectionData: String?) : NamedObject(objectId, columnName) {

    override val databasePointRef: DatabaseReference?
        get() {
            if (tracker != null) {
                return FirebaseHelper.dbRef
                        ?.child(FirebaseHelper.CHILD_NAME_TRACKERS)
                        ?.child(tracker!!.objectId)
                        ?.child(FirebaseHelper.CHILD_NAME_ATTRIBUTES)
                        ?.child(objectId)
            } else {
                return null
            }
        }

    override fun makeNewObjectId(): String {
        if (tracker != null) {
            return FirebaseHelper.generateAttributeKey(tracker!!.objectId)
        } else return UUID.randomUUID().toString()
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
        const val TYPE_AUDIO = 9

        private val permissionDict = SparseArray<Array<String>>()

        init {
            permissionDict.put(TYPE_LOCATION, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionDict.put(TYPE_IMAGE, arrayOf(Manifest.permission.CAMERA))
            permissionDict.put(TYPE_AUDIO, arrayOf(Manifest.permission.RECORD_AUDIO))
        }

        fun getPermissionsForAttribute(typeId: Int): Array<String>? {
            return permissionDict[typeId]
        }


        fun createAttribute(objectId: String?, parent: OTTracker?, columnName: String, isRequired: Boolean, typeId: Int, propertyData: String?, connectionData: String?): OTAttribute<out Any> {
            val attr = when (typeId) {
                TYPE_NUMBER -> OTNumberAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_TIME -> OTTimeAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_TIMESPAN -> OTTimeSpanAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_SHORT_TEXT -> OTShortTextAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_LONG_TEXT -> OTLongTextAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_LOCATION -> OTLocationAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_CHOICE -> OTChoiceAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_RATING -> OTRatingAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_IMAGE -> OTImageAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_AUDIO -> OTAudioRecordAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
                else -> OTNumberAttribute(objectId, parent, columnName, isRequired, propertyData, connectionData)
            }
            return attr
        }

        fun createAttribute(tracker: OTTracker, columnName: String, typeId: Int): OTAttribute<out Any> {
            return createAttribute(FirebaseHelper.generateAttributeKey(tracker.objectId), tracker, columnName, false, typeId, null, null)
        }
    }

    fun requiredPermissions(): Array<String>? {
        return getPermissionsForAttribute(typeId)
    }


    var tracker: OTTracker? by Delegates.observable(parentTracker) {
        prop, old, new ->
        if (old != new) {
            if (old != null) {
                removedFromTracker.invoke(this, old)
            }

            if (new != null) {
                addedToTracker.invoke(this, new)
            }
        }
    }


    var isRequired: Boolean = isRequired
        set(value) {
            if (field != value) {
                field = value
                databasePointRef?.child("is_required")?.setValue(value)
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
            databasePointRef?.child("connection_serialized")?.setValue(new?.getSerializedString())
        }
    }

    constructor(parentTracker: OTTracker?, columnName: String, typeId: Int) : this(null, parentTracker, columnName, false, typeId, null, null)

    init {
        suspendDatabaseSync = true
        createProperties()
        if (propertyData != null) {

            integerKeyEntryParser.fromJson(propertyData, Array<SerializedIntegerKeyEntry>::class.java).forEach {
                setPropertyValueFromSerializedString(it.key, it.value)
            }
        }

        if (connectionData != null) {
            valueConnection = OTConnection(connectionData)
        }
        suspendDatabaseSync = false
    }

    protected abstract fun createProperties()

    open fun compareValues(a: Any, b: Any): Int {
        return 0
    }

    fun getSerializedProperties(): String {
        return integerKeyEntryParser.toJson(
                propertyKeys.map {
                    SerializedIntegerKeyEntry(it, getProperty<Any>(it).getSerializedValue())
                }.toTypedArray()
        )
    }

    abstract val typeNameForSerialization: String

    protected fun assignProperty(property: OTProperty<out Any>) {
        property.onValueChanged += {
            sender, args ->
            onPropertyValueChanged(args)
        }

        settingsProperties.put(property.key, property)
    }

    protected open fun onPropertyValueChanged(args: OTProperty.PropertyChangedEventArgs<out Any>) {
        propertyValueChanged.invoke(this, args)
        databasePointRef?.child("property_serialized")?.setValue(getSerializedProperties())
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

    abstract fun formatAttributeValue(value: Any): CharSequence

    /***
     * Autocompleted values based on attribute-specific settings.
     * [return] whether the method finished synchronously.
     */
    abstract fun getAutoCompleteValue(): Observable<DataType>

    fun makeAutoCompleteValueWithId(id: Int): Observable<Pair<Int, Any>> {
        return getAutoCompleteValue().map {
            data ->
            Pair(id, data as Any)
        }
    }



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

        val target: TextView = recycledView as? TextView ?: TextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.viewForItemListTextAppearance)

        target.background = null

        return target
    }

    open fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is TextView) {
            if (value != null) {
                view.text = TextHelper.stringWithFallback(formatAttributeValue(value), "-")
            } else {
                view.text = view.context.getString(R.string.msg_empty_value)
            }
            return true
        } else return false
    }

    open fun getRecommendedChartModels(): Array<ChartModel<*>> {
        return arrayOf()
    }

    fun isMeasureFactoryConnected(measureFactory: OTMeasureFactory): Boolean {
        return valueConnection?.source?.factory?.typeCode == measureFactory.typeCode
    }

    fun isConnectionValid(invalidMessages: MutableList<CharSequence>?): Boolean {
        val connection = valueConnection
        if (connection != null) {
            println("OMNITRACK attribute name: ${name}, tracker: ${tracker?.name}, source: ${connection.source}")
            val service = connection.source!!.factory.service
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                return true
            } else {
                invalidMessages?.add(TextHelper.fromHtml(String.format(
                        "<font color=\"blue\">${OTApplication.app.resources.getString(R.string.msg_service_is_not_activated_format)}</font>",
                        OTApplication.app.resources.getString(service.nameResourceId))))
                return false
            }
        } else return true
    }
}