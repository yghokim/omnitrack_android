package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.NumberInputView
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTNumberAttributeHelper : OTAttributeHelper() {

    companion object {
        const val NUMBERSTYLE = "style"
    }

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(true, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_number_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_number
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

    override val propertyKeys: Array<String>
        get() = arrayOf(NUMBERSTYLE)

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            NUMBERSTYLE ->
                OTPropertyManager.getHelper(OTPropertyManager.EPropertyType.NumberStyle)

            else -> throw IllegalArgumentException("Unsupported property key.")
        } as OTPropertyHelper<T>
    }

    private fun getNumberStyle(attribute: OTAttributeDAO): NumberStyle? {
        return getDeserializedPropertyValue(NUMBERSTYLE, attribute)
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            NUMBERSTYLE -> NumberStyle()
            else -> null
        }
    }


    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_NUMBER

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is NumberInputView) {
            inputView.numberStyle = getNumberStyle(attribute) ?: NumberStyle()
        }
    }

    override fun isIntrinsicDefaultValueSupported(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun makeIntrinsicDefaultValue(attribute: OTAttributeDAO): Observable<out Any> {
        return Observable.just(0)
    }

    override fun makeIntrinsicDefaultValueMessage(attribute: OTAttributeDAO): CharSequence {
        return OTApplication.getString(R.string.msg_intrinsic_number)
    }
}