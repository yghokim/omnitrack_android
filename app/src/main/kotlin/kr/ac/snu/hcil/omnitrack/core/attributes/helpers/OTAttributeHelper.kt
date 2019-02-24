package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import android.view.View
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import java.util.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

/**
 * Created by Young-Ho on 10/7/2017.
 */
abstract class OTAttributeHelper(protected val context: Context) {

    protected val propertyManager: OTPropertyManager
        get() = (context.applicationContext as OTAndroidApp).applicationComponent.getPropertyManager()

    val FALLBACK_POLICY_RESOLVER_EMPTY_VALUE = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_empty_value) {
        override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
            return Single.just<Nullable<out Any>>(Nullable(null)).observeOn(AndroidSchedulers.mainThread())
        }
    }

    val FALLBACK_POLICY_RESOLVER_PREVIOUS_VALUE = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_attribute_fallback_policy_last, isValueVolatile = true) {
        override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {

            return Single.defer {
                var previousValue: Any? = null
                try {
                    /*
                    previousValue= realm.where(OTItemValueEntryDAO::class.java)
                            .equalTo("key", attribute.localId)
                            .equalTo("items.trackerId", attribute.trackerId)
                            .equalTo("items.removed", false)
                            .sort("items.timestamp", Sort.DESCENDING)
                            .findAll().filter { it.value != null }.first()?.value?.let{
                        TypeStringSerializationHelper.deserialize(it)
                    }*/


                    val list = realm.where(OTItemDAO::class.java)
                            .equalTo(BackendDbManager.FIELD_TRACKER_ID, attribute.trackerId)
                            .equalTo("fieldValueEntries.key", attribute.localId)
                            .equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
                            .sort("timestamp", Sort.DESCENDING)
                            .findAll()
                    if (list.count() > 0) {
                        for (item in list) {
                            val value = item.getValueOf(attribute.localId)
                            if (value != null) {
                                previousValue = value
                                break
                            }
                        }
                    } else previousValue = null

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    previousValue = null
                }

                return@defer Single.just<Nullable<out Any>>(Nullable(previousValue))
            }.subscribeOn(AndroidSchedulers.mainThread())
        }
    }

    abstract val typeNameForSerialization: String

    open val supportedFallbackPolicies = LinkedHashMap<Int, FallbackPolicyResolver>().apply{
        put(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL, FALLBACK_POLICY_RESOLVER_EMPTY_VALUE)
        put(OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM, FALLBACK_POLICY_RESOLVER_PREVIOUS_VALUE)
    }

    open val propertyKeys: Array<String> = emptyArray()

    open fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    open fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.drawable.field_icon_shorttext

    open fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int = R.drawable.icon_small_shorttext
    open fun isExternalFile(attribute: OTAttributeDAO): Boolean = false
    open fun getRequiredPermissions(attribute: OTAttributeDAO?): Array<String>? = null

    open fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return emptyArray()
    }

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
    }

    open fun makePropertyViews(context: Context): Collection<Pair<String, View>> {
        val result = ArrayList<Pair<String, View>>()
        for (key in propertyKeys) {
            result.add(Pair(key, makePropertyView(key, context)))
        }
        return result
    }

    //Input Values=======================================================================================================

    open fun isAttributeValueVolatile(attribute: OTAttributeDAO): Boolean {
        return attribute.serializedConnection?.let { (context.applicationContext as OTAndroidApp).applicationComponent.getConnectionTypeAdapter().fromJson(it) }?.source != null
                || supportedFallbackPolicies[attribute.fallbackValuePolicy]?.isValueVolatile == true
    }

    open fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
        return Single.defer{
            val resolver = supportedFallbackPolicies[attribute.fallbackValuePolicy]
            if(resolver==null)
            {
                return@defer Single.just<Nullable<out Any>>(Nullable(null))
            }
            else{
                return@defer resolver.getFallbackValue(attribute, realm)
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
    open fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        return value.toString()
    }

    open fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE
    }

    open fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {

        val target: TextView = recycledView as? TextView ?: TextView(context)

        InterfaceHelper.setTextAppearance(target, R.style.viewForItemListTextAppearance)

        target.background = null

        return target
    }

    open fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer<Boolean> {
            if (view is TextView) {
                if (value != null) {
                    view.text = TextHelper.stringWithFallback(formatAttributeValue(attribute, value), "-")
                } else {
                    view.text = view.context.getString(R.string.msg_empty_value)
                }
                Single.just(true)
            } else Single.just(false)
        }
    }

    //Configuration=======================================================================================
    open fun initialize(attribute: OTAttributeDAO) {
        //noop
    }

    //Export====================
    open fun getAttributeUniqueName(attribute: OTAttributeDAO): String {
        return "${attribute.name}(${attribute.localId})"
    }

    open fun onAddColumnToTable(attribute: OTAttributeDAO, out: MutableList<String>) {
        out.add(getAttributeUniqueName(attribute))
    }

    open fun onAddValueToTable(attribute: OTAttributeDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (value != null) {
            out.add(formatAttributeValue(attribute, value).toString())
        } else out.add(null)
    }

    //Chart======================
    open fun makeRecommendedChartModels(attribute: OTAttributeDAO, realm: Realm): Array<ChartModel<*>> {
        return emptyArray()
    }
}