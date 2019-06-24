package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.convertNumericToDouble
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.NumericSorter
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.NumberStyle
import java.math.BigDecimal

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTNumberFieldHelper(context: Context) : OTFieldHelper(context), ISingleNumberFieldHelper {

    companion object {
        const val NUMBERSTYLE = "style"
        const val BUTTON_UNIT = "buttonUnit"
    }

    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply{
            this[OTFieldDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = object : FallbackPolicyResolver(context.applicationContext, R.string.msg_intrinsic_number, false) {
                override fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.just(Nullable(0))
                }

            }
        }

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_number_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return R.drawable.icon_small_number
    }

    override fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return arrayOf(NumericSorter(field.name, field.localId))
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override val propertyKeys: Array<String>
        get() = arrayOf(NUMBERSTYLE, BUTTON_UNIT)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            NUMBERSTYLE ->
                propertyManager.getHelper(OTPropertyManager.EPropertyType.NumberStyle)
            BUTTON_UNIT ->
                propertyManager.getHelper(OTPropertyManager.EPropertyType.Number)

            else -> throw IllegalArgumentException("Unsupported property key.")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            NUMBERSTYLE -> ""
            BUTTON_UNIT -> context.getString(R.string.msg_number_button_unit)
            else -> ""
        }
    }

    fun getNumberStyle(field: OTFieldDAO): NumberStyle? {
        return getDeserializedPropertyValue(NUMBERSTYLE, field)
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            NUMBERSTYLE -> NumberStyle()
            BUTTON_UNIT -> BigDecimal.ONE
            else -> null
        }
    }

    override fun convertValueToSingleNumber(value: Any, field: OTFieldDAO): Double {
        return convertNumericToDouble(value)
    }
}