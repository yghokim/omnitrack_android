package kr.ac.snu.hcil.omnitrack.core.attributes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.SparseArray
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.tbruyelle.rxpermissions.RxPermissions
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTProperty
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import rx.Observable
import rx.Single
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class OTAttribute<DataType>(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, val typeId: Int, propertyData: Map<String, Any?>?, connectionData: String?) : NamedObject(objectId, columnName) {

    /*
    override val databasePointRef: DatabaseReference?
        get() {
            if (tracker != null) {
                return DatabaseManager.dbRef
                        ?.child(DatabaseManager.CHILD_NAME_TRACKERS)
                        ?.child(tracker!!.objectId)
                        ?.child(DatabaseManager.CHILD_NAME_ATTRIBUTES)
                        ?.child(objectId)
            } else {
                return null
            }
        }
        */

    override fun makeNewObjectId(): String {
        /*
        if (tracker != null) {
            return DatabaseManager.generateAttributeKey(tracker!!.objectId)
        } else return UUID.randomUUID().toString()*/
        return UUID.randomUUID().toString()
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

        fun getTypeString(typeId: Int): String {
            return when (typeId) {
                TYPE_NUMBER -> "number"
                TYPE_TIME -> "time"
                TYPE_TIMESPAN -> "timespan"
                TYPE_SHORT_TEXT -> "short_text"
                TYPE_LONG_TEXT -> "long_text"
                TYPE_LOCATION -> "location"
                TYPE_CHOICE -> "choice"
                TYPE_RATING -> "rating"
                TYPE_IMAGE -> "image"
                TYPE_AUDIO -> "audio"
                else -> throw IllegalArgumentException("Unsupported attribute type: ${typeId}")
            }
        }

        fun getTypeIdFromString(typeString: String): Int {
            return when (typeString.toLowerCase()) {
                "number" -> TYPE_NUMBER
                "time" -> TYPE_TIME
                "timespan" -> TYPE_TIMESPAN
                "short_text" -> TYPE_SHORT_TEXT
                "long_text" -> TYPE_LONG_TEXT
                "location" -> TYPE_LOCATION
                "choice" -> TYPE_CHOICE
                "rating" -> TYPE_RATING
                "image" -> TYPE_IMAGE
                "audio" -> TYPE_AUDIO
                else -> throw IllegalArgumentException("Unsupported attribute type string: ${typeString}")
            }
        }

        private val permissionDict = SparseArray<Array<String>>()

        init {
            permissionDict.put(TYPE_LOCATION, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionDict.put(TYPE_IMAGE, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionDict.put(TYPE_AUDIO, arrayOf(Manifest.permission.RECORD_AUDIO))
        }

        fun getPermissionsForAttribute(typeId: Int): Array<String>? {
            return permissionDict[typeId]
        }


        fun createAttribute(objectId: String?, localKey: Int?, parent: OTTracker?, columnName: String, isRequired: Boolean, typeId: Int, propertyData: Map<String, Any?>?, connectionData: String?): OTAttribute<out Any> {
            val attr = when (typeId) {
                TYPE_NUMBER -> OTNumberAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_TIME -> OTTimeAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_TIMESPAN -> OTTimeSpanAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_SHORT_TEXT -> OTShortTextAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_LONG_TEXT -> OTLongTextAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_LOCATION -> OTLocationAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_CHOICE -> OTChoiceAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_RATING -> OTRatingAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_IMAGE -> OTImageAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                TYPE_AUDIO -> OTAudioRecordAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
                else -> OTNumberAttribute(objectId, localKey, parent, columnName, isRequired, propertyData, connectionData)
            }
            return attr
        }

        fun createAttribute(tracker: OTTracker, columnName: String, typeId: Int): OTAttribute<out Any> {
            return createAttribute(null, tracker.nextAttributeLocalKey(), tracker, columnName, false, typeId, null, null)
        }

        fun showPermissionCheckDialog(activity: Activity, typeId: Int, typeName: String, onGranted: (Boolean) -> Unit, onDenied: (() -> Unit)? = null): MaterialDialog? {
            val requiredPermissions = OTAttribute.getPermissionsForAttribute(typeId)
            if (requiredPermissions != null) {
                val notGrantedPermissions = requiredPermissions.filter { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }
                if (notGrantedPermissions.isNotEmpty()) {
                    val dialog = DialogHelper.makeYesNoDialogBuilder(activity, activity.resources.getString(R.string.msg_permission_required),
                            String.format(activity.resources.getString(R.string.msg_format_permission_request_of_field), typeName),
                            cancelable = false,
                            onYes = {
                                val rxPermissions = RxPermissions(activity)
                                rxPermissions.request(*requiredPermissions)
                                        .subscribe {
                                            granted ->
                                            if (granted) {
                                                onGranted.invoke(true)
                                            } else {
                                                onDenied?.invoke()
                                            }
                                        }
                            },
                            onCancel = null,
                            yesLabel = R.string.msg_allow_permission,
                            noLabel = R.string.msg_cancel
                    )
                    return dialog.show()
                } else {
                    onGranted.invoke(false)
                    return null
                }
            } else {
                onGranted.invoke(false)
                return null
            }
        }
    }

    var tracker: OTTracker? by Delegates.observable(parentTracker) {
        prop, old, new ->
        if (old != new) {
            if (old != null) {
                removedFromTracker.invoke(this, old)
            }

            if (new != null) {
                //this.localKey = new.nextAttributeLocalKey()
                addedToTracker.invoke(this, new)
            }

            if (!suspendDatabaseSync) save()
        }
    }

    var localKey: Int = localKey ?: parentTracker?.nextAttributeLocalKey() ?: -1
        private set(value) {
            if (field != value) {
                field = value

                if (!suspendDatabaseSync) save()
            }
        }

    var isRequired: Boolean = isRequired
        set(value) {
            if (field != value) {
                field = value
                if (!suspendDatabaseSync) save()
            }
        }

    var intrinsicPosition: Int = 0
        set(value) {
            if (field != value) {
                field = value

                if (!suspendDatabaseSync) save()
            }
        }

    /**
     * whether default value is stable or dynamic to time
     * */

    val removedFromTracker = Event<OTTracker>()
    val addedToTracker = Event<OTTracker>()

    abstract val valueNumericCharacteristics: NumericCharacteristics

    abstract val typeNameResourceId: Int

    abstract val typeSmallIconResourceId: Int

    open val isAutoCompleteValueStatic: Boolean = true

    open val isExternalFile: Boolean = false

    abstract val typeNameForSerialization: String

    fun requiredPermissions(): Array<String>? {
        return getPermissionsForAttribute(typeId)
    }

    abstract val propertyKeys: Array<String>
    val propertyValueChanged = Event<OTProperty.PropertyChangedEventArgs<out Any>>()
    private val settingsProperties = HashMap<String, OTProperty<out Any>>()

    val propertyValueChangedSubject = SerializedSubject(PublishSubject.create<Pair<OTAttribute<DataType>, OTProperty.PropertyChangedEventArgs<out Any>>>())

    var valueConnection: OTConnection? by Delegates.observable(null as OTConnection?) {
        prop, old, new ->
        if (old != new) {
            if (!suspendDatabaseSync) save()
        }
    }

    constructor(parentTracker: OTTracker?, columnName: String, typeId: Int) : this(null, null, parentTracker, columnName, false, typeId, null, null)

    init {
        suspendDatabaseSync = true
        createProperties()
        /*
        if (propertyData != null) {
            integerKeyEntryParser.fromJson(propertyData, Array<SerializedIntegerKeyEntry>::class.java).forEach {
                setPropertyValueFromSerializedString(it.key, it.value)
            }
        }*/

        if (propertyData != null)
            readPropertiesFromDatabase(propertyData)

        if (connectionData != null) {
            valueConnection = OTConnection(connectionData)
        }

        if (localKey == null && parentTracker != null) {
            this.localKey = parentTracker.nextAttributeLocalKey()
        }
        suspendDatabaseSync = false
    }

    protected abstract fun createProperties()

    open fun compareValues(a: Any, b: Any): Int {
        return 0
    }
/*
    fun getSerializedProperties(): String {
        println(propertyKeys)
        return integerKeyEntryParser.toJson(
                propertyKeys.map {
                    SerializedIntegerKeyEntry(it, getProperty<Any>(it).getSerializedValue())
                }.toTypedArray()
        )
    }*/

    fun writePropertiesToDatabase(ref: MutableMap<String, String>) {
        propertyKeys.forEach {
            it ->
            ref[it] = getProperty<Any>(it).getSerializedValue()
        }
    }

    fun readPropertiesFromDatabase(ref: Map<String, Any?>) {
        for (child in ref.entries) {
            setPropertyValueFromSerializedString(child.key, child.value as String)
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
        propertyValueChangedSubject.onNext(Pair(this, args))
        if (!suspendDatabaseSync) {
            save()
        }
    }

    fun <T> getProperty(key: String): OTProperty<T> {
        @Suppress("UNCHECKED_CAST")
        return settingsProperties[key]!! as OTProperty<T>
    }

    fun <T> getPropertyValue(key: String): T {
        return getProperty<T>(key).value
    }

    fun setPropertyValue(key: String, value: Any): Boolean {
        try {
            val prop = getProperty<Any>(key)
            if (prop.value != value) {
                prop.value = value
                return true
            } else return false
        } catch(ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    fun setPropertyValueFromSerializedString(key: String, serializedValue: String) {
        try {
            getProperty<Any>(key).setValueFromSerializedString(serializedValue)
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
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

    open fun makePropertyViews(context: Context): Collection<ReadOnlyPair<String, View>> {
        val result = ArrayList<ReadOnlyPair<String, View>>()
        for (key in propertyKeys) {
            result.add(ReadOnlyPair(key, getProperty<Any>(key).buildView(context)))
        }
        return result
    }

    //reuse recycled view if possible.
    open fun getInputView(context: Context, previewMode: Boolean, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == getInputViewType(previewMode))) {
                    recycledView
                } else {
                    AAttributeInputView.makeInstance(getInputViewType(previewMode), context)
                }

        refreshInputViewUI(view)
        view.previewMode = previewMode
        return view
    }

    abstract fun refreshInputViewUI(inputView: AAttributeInputView<out Any>)

    open fun getViewForItemListContainerType(): Int {
        return VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE
    }

    open fun getViewForItemList(context: Context, recycledView: View?): View {

        val target: TextView = recycledView as? TextView ?: TextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.viewForItemListTextAppearance)

        target.background = null

        return target
    }

    open fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        return Single.defer<Boolean> {
            if (view is TextView) {
                if (value != null) {
                    view.text = TextHelper.stringWithFallback(formatAttributeValue(value), "-")
                } else {
                    view.text = view.context.getString(R.string.msg_empty_value)
                }
                Single.just(true)
            } else Single.just(false)
        }
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
            val source = connection.source
            if (source != null) {
                val service = source.factory.getService()
                if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                    return true
                } else {
                    invalidMessages?.add(TextHelper.fromHtml(String.format(
                            "<font color=\"blue\">${OTApplication.app.resourcesWrapped.getString(R.string.msg_service_is_not_activated_format)}</font>",
                            OTApplication.app.resourcesWrapped.getString(service.nameResourceId))))
                    return false
                }
            } else {
                invalidMessages?.add(TextHelper.fromHtml(
                        "<font color=\"blue\">Connection is not supported on current version.</font>"
                ))
                return false
            }
        } else return true
    }

    protected fun getAttributeUniqueName(): String {
        return "${name}(${objectId})"
    }

    open fun onAddColumnToTable(out: MutableList<String>) {
        out.add(getAttributeUniqueName())
    }

    open fun onAddValueToTable(value: Any?, out: MutableList<String?>, uniqKey: String?) {
        val str = value?.toString()
        if (str.isNullOrBlank()) {
            out.add(null)
        } else out.add(str)
    }

    override fun save() {
        OTApplication.app.databaseManager.saveAttribute(tracker?.objectId, this as OTAttribute<out Any>, intrinsicPosition)
    }
}