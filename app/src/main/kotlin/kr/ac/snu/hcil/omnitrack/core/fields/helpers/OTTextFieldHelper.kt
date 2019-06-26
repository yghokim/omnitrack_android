package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AlphabeticalSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.TextLengthSorter
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyHelper
import kr.ac.snu.hcil.omnitrack.core.fields.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTTextFieldHelper(context: Context) : OTFieldHelper(context) {

    companion object {
        const val PROPERTY_INPUT_TYPE = "type"

        const val INPUT_TYPE_SHORT = 0
        const val INPUT_TYPE_LONG = 1
    }

    override val propertyKeys: Array<String> = arrayOf(PROPERTY_INPUT_TYPE)

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics {
        return NumericCharacteristics(false, false)
    }

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_shorttext_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return when (getInputType(field)) {
            INPUT_TYPE_SHORT -> R.drawable.icon_small_shorttext
            INPUT_TYPE_LONG -> R.drawable.icon_small_longtext
            else -> R.drawable.icon_small_shorttext
        }
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_STRING

    fun getInputType(field: OTFieldDAO): Int {
        return getDeserializedPropertyValue<Int>(PROPERTY_INPUT_TYPE, field) ?: INPUT_TYPE_SHORT
    }

    override fun <T> getPropertyHelper(propertyKey: String): OTPropertyHelper<T> {
        return when (propertyKey) {
            PROPERTY_INPUT_TYPE -> propertyManager.getHelper(OTPropertyManager.EPropertyType.Selection)
            else -> throw IllegalArgumentException("Unsupported property type $propertyKey")
        } as OTPropertyHelper<T>
    }

    override fun getPropertyInitialValue(propertyKey: String): Any? {
        return when (propertyKey) {
            PROPERTY_INPUT_TYPE -> INPUT_TYPE_SHORT
            else -> null
        }
    }

    override fun getPropertyTitle(propertyKey: String): String {
        return when (propertyKey) {
            PROPERTY_INPUT_TYPE -> context.resources.getString(R.string.msg_text_field_property_input_type)
            else -> ""
        }
    }

    override fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return arrayOf(
                AlphabeticalSorter("${field.name} Alphabetical", field.localId),
                TextLengthSorter("${field.name} Length", field.localId)
        )
    }
}