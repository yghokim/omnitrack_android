package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.NumericSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.convertNumericToDouble
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.math.BigDecimal

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTNumberAttributeHelper(configuredContext: ConfiguredContext) : OTAttributeHelper(configuredContext), ISingleNumberAttributeHelper {

    companion object {
        const val NUMBERSTYLE = "style"
        const val BUTTON_UNIT = "buttonUnit"
    }

    override val supportedFallbackPolicies: LinkedHashMap<Int, FallbackPolicyResolver>
        get() = super.supportedFallbackPolicies.apply{
            this[OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = object: FallbackPolicyResolver(R.string.msg_intrinsic_number, false){
                override fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>> {
                    return Single.just(Nullable(0))
                }

            }
        }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_number_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_number
    }

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(NumericSorter(attribute.name, attribute.localId))
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override val propertyKeys: Array<String>
        get() = arrayOf(NUMBERSTYLE, BUTTON_UNIT)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            NUMBERSTYLE ->
                OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.NumberStyle)
            BUTTON_UNIT ->
                OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.Number)

            else -> throw IllegalArgumentException("Unsupported property key.")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            NUMBERSTYLE -> ""
            BUTTON_UNIT -> configuredContext.applicationComponent.applicationContext().getString(R.string.msg_number_button_unit)
            else -> ""
        }
    }

    private fun getNumberStyle(attribute: OTAttributeDAO): NumberStyle? {
        return getDeserializedPropertyValue(NUMBERSTYLE, attribute)
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            NUMBERSTYLE -> NumberStyle()
            BUTTON_UNIT -> BigDecimal.ONE
            else -> null
        }
    }


    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_NUMBER

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = getNumberStyle(attribute) ?: NumberStyle()
            inputView.moveUnit = getDeserializedPropertyValue<BigDecimal>(BUTTON_UNIT, attribute) ?: BigDecimal.ONE
        }
    }

    override fun convertValueToSingleNumber(value: Any, attribute: OTAttributeDAO): Double {
        return convertNumericToDouble(value)
    }
}