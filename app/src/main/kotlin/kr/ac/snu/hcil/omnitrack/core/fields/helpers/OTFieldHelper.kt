package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.fields.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kotlin.collections.set

/**
 * Created by Young-Ho on 10/7/2017.
 */
abstract class OTFieldHelper(protected val context: Context) {

    val propertyManager: OTPropertyManager
        get() = (context.applicationContext as OTAndroidApp).applicationComponent.getPropertyManager()

    val FALLBACK_POLICY_RESOLVER_EMPTY_VALUE = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_empty_value) {
        override fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {
            return Single.just<Nullable<out Any>>(Nullable(null)).observeOn(AndroidSchedulers.mainThread())
        }
    }

    val FALLBACK_POLICY_RESOLVER_PREVIOUS_VALUE = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_attribute_fallback_policy_last, isValueVolatile = true) {
        override fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {

            return Single.defer {
                var previousValue: Any? = null
                try {
                    /*
                    previousValue= realm.where(OTItemValueEntryDAO::class.java)
                            .equalTo("key", field.localId)
                            .equalTo("items.trackerId", field.trackerId)
                            .equalTo("items.removed", false)
                            .sort("items.timestamp", Sort.DESCENDING)
                            .findAll().filter { it.value != null }.first()?.value?.let{
                        TypeStringSerializationHelper.deserialize(it)
                    }*/


                    val list = realm.where(OTItemDAO::class.java)
                            .equalTo(BackendDbManager.FIELD_TRACKER_ID, field.trackerId)
                            .equalTo("fieldValueEntries.key", field.localId)
                            .equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
                            .sort("timestamp", Sort.DESCENDING)
                            .findAll()
                    if (list.count() > 0) {
                        for (item in list) {
                            val value = item.getValueOf(field.localId)
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

    open val supportedFallbackPolicies: LinkedHashMap<String, FallbackPolicyResolver> = LinkedHashMap<String, FallbackPolicyResolver>().apply {
        put(OTFieldDAO.DEFAULT_VALUE_POLICY_NULL, FALLBACK_POLICY_RESOLVER_EMPTY_VALUE)
        put(OTFieldDAO.DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM, FALLBACK_POLICY_RESOLVER_PREVIOUS_VALUE)
    }

    open val propertyKeys: Array<String> = emptyArray()

    open fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    open fun getTypeNameResourceId(field: OTFieldDAO): Int = R.drawable.field_icon_shorttext

    open fun getTypeSmallIconResourceId(field: OTFieldDAO): Int = R.drawable.icon_small_shorttext
    open fun isExternalFile(field: OTFieldDAO): Boolean = false
    open fun getRequiredPermissions(field: OTFieldDAO?): Array<String>? = null

    open fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return emptyArray()
    }

    open fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T {
        return getPropertyHelper<T>(propertyKey).parseValue(serializedValue)
    }

    fun <T> setPropertyValue(propertyKey: String, value: T, field: OTFieldDAO, realm: Realm) {
        field.setPropertySerializedValue(propertyKey, getPropertyHelper<T>(propertyKey).getSerializedValue(value))
    }

    fun <T> getDeserializedPropertyValue(propertyKey: String, field: OTFieldDAO): T? {
        val s = field.getPropertySerializedValue(propertyKey)
        return if (s != null)
            parsePropertyValue<T>(propertyKey, s)
        else null
    }

    open fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        throw UnsupportedOperationException("Not supported operation.")
    }

    fun getDeserializedPropertyTable(field: OTFieldDAO): Map<String, Any?> {
        val table = HashMap<String, Any?>()
        propertyKeys.forEach { key ->
            val serialized = field.getPropertySerializedValue(key)
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

    //Input Values=======================================================================================================

    open fun isAttributeValueVolatile(field: OTFieldDAO): Boolean {
        return field.serializedConnection?.let { (context.applicationContext as OTAndroidApp).applicationComponent.getConnectionTypeAdapter().fromJson(it) }?.source != null
                || supportedFallbackPolicies[field.fallbackValuePolicy]?.isValueVolatile == true
    }

    open fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {
        return Single.defer{
            val resolver = supportedFallbackPolicies[field.fallbackValuePolicy]
            if(resolver==null)
            {
                return@defer Single.just<Nullable<out Any>>(Nullable(null))
            }
            else{
                return@defer resolver.getFallbackValue(field, realm)
            }
        }
    }

    open fun formatAttributeValue(field: OTFieldDAO, value: Any): CharSequence {
        return value.toString()
    }

    //Configuration=======================================================================================
    open fun initialize(field: OTFieldDAO) {
        //noop
    }

    //Export====================
    open fun getAttributeUniqueName(field: OTFieldDAO): String {
        return "${field.name}(${field.localId})"
    }

    open fun onAddColumnToTable(field: OTFieldDAO, out: MutableList<String>) {
        out.add(getAttributeUniqueName(field))
    }

    open fun onAddValueToTable(field: OTFieldDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (value != null) {
            out.add(formatAttributeValue(field, value).toString())
        } else out.add(null)
    }

    //Chart======================
    open fun makeRecommendedChartModels(field: OTFieldDAO, realm: Realm): Array<ChartModel<*>> {
        return emptyArray()
    }
}