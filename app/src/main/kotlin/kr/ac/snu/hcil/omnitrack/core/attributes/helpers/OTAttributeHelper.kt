package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.view.View
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.util.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * Created by Young-Ho on 10/7/2017.
 */
abstract class OTAttributeHelper {

    open fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    open fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.drawable.field_icon_shorttext

    open fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int = R.drawable.icon_small_shorttext
    open fun isIntrinsicDefaultValueVolatile(attribute: OTAttributeDAO): Boolean = false
    open fun isExternalFile(attribute: OTAttributeDAO): Boolean = false
    open fun getRequiredPermissions(attribute: OTAttributeDAO): Array<String>? = null
    abstract val typeNameForSerialization: String

    open val propertyKeys: Array<String> = emptyArray()

    open fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T {
        return getPropertyHelper<T>(propertyKey).parseValue(serializedValue)
    }

    fun <T> setPropertyValue(propertyKey: String, value: T, attribute: OTAttributeDAO, realm: Realm) {
        attribute.setPropertySerializedValue(propertyKey, getPropertyHelper<T>(propertyKey).getSerializedValue(value))
    }

    fun <T> getDeserializedPropertyValue(propertyKey: String, attribute: OTAttributeDAO): T? {
        val s = attribute.getPropertySerializedValue(propertyKey)
        return if (s != null)
            parsePropertyValue<T>(propertyKey, s)
        else null
    }

    open fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        throw UnsupportedOperationException("Not supported operation.")
    }

    fun getDeserializedPropertyTable(attribute: OTAttributeDAO): Map<String, Any?> {
        val table = HashMap<String, Any?>()
        propertyKeys.forEach { key ->
            val serialized = attribute.getPropertySerializedValue(key)
            if (serialized != null) {
                table[key] = parsePropertyValue(key, serialized)
            }
        }

        return table
    }

    open fun getPropertyTitle(propertyKey: String): String {
        return ""
    }

    open fun getPropertyInitialValue(propertyKey: String): Any? {
        throw IllegalAccessException("Must not reach here.")
    }

    open fun makePropertyView(propertyKey: String, context: Context): APropertyView<out Any> {
        val view = getPropertyHelper<Any>(propertyKey).makeView(context)
        view.title = getPropertyTitle(propertyKey)
        val initialValue = getPropertyInitialValue(propertyKey)
        if (initialValue != null)
            view.value = initialValue
        return view

        return view
    }

    open fun makePropertyViews(context: Context): Collection<ReadOnlyPair<String, View>> {
        val result = ArrayList<ReadOnlyPair<String, View>>()
        for (key in propertyKeys) {
            result.add(ReadOnlyPair(key, makePropertyView(key, context)))
        }
        return result
    }

    //Input Values=======================================================================================================
    open fun isIntrinsicDefaultValueSupported(attribute: OTAttributeDAO): Boolean {
        return false
    }

    open fun makeIntrinsicDefaultValue(attribute: OTAttributeDAO): Observable<out Any> {
        return Observable.empty()
    }

    open fun makeIntrinsicDefaultValueMessage(attribute: OTAttributeDAO): CharSequence {
        return ""
    }

    open fun isAttributeValueVolatile(attribute: OTAttributeDAO): Boolean {
        return attribute.serializedConnection?.let { OTConnection.fromJson(it) }?.source != null
                || (attribute.fallbackValuePolicy == OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM)
                || (attribute.fallbackValuePolicy == OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE && isIntrinsicDefaultValueSupported(attribute) && isIntrinsicDefaultValueVolatile(attribute))
    }

    open fun getFallbackValue(attribute: OTAttributeDAO): Observable<Nullable<out Any>> {
        return Observable.defer {
            println("getFallbackValue. Current thread: ${Thread.currentThread().name}")
            when (attribute.fallbackValuePolicy) {
                OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE -> {
                    return@defer if (isIntrinsicDefaultValueSupported(attribute)) {
                        makeIntrinsicDefaultValue(attribute).map { value -> Nullable(value) as Nullable<out Any> }
                    } else Observable.just<Nullable<out Any>>(Nullable(null))
                }
                OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM -> {
                    return@defer Observable.just<Nullable<out Any>>(Nullable(null))

                }
                OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_PRESET -> {
                    return@defer attribute.fallbackPresetSerializedValue?.let {
                        Observable.just(Nullable(TypeStringSerializationHelper.deserialize(it)) as Nullable<out Any>)
                    } ?: Observable.just<Nullable<out Any>>(Nullable(null))
                }
                OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL -> {
                    return@defer Observable.just<Nullable<out Any>>(Nullable(null))
                }
                else -> {
                    return@defer Observable.just<Nullable<out Any>>(Nullable(null))
                }
            }
        }
    }

    //Input View=========================================================================================================
    abstract fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int

    open fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {}
    //reuse recycled view if possible.
    open fun getInputView(context: Context, previewMode: Boolean, attribute: OTAttributeDAO, recycledView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
        val view =
                if ((recycledView?.typeId == getInputViewType(previewMode, attribute))) {
                    recycledView
                } else {
                    AAttributeInputView.makeInstance(getInputViewType(previewMode, attribute), context)
                }

        refreshInputViewUI(view, attribute)
        view.previewMode = previewMode
        return view
    }

    //Item list view==========================================================================================================================
    open fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE
    }

    //Configuration=======================================================================================
    open fun initialize(attribute: OTAttributeDAO) {
        //noop
    }
}