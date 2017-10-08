package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView

/**
 * Created by Young-Ho on 10/7/2017.
 */
abstract class OTAttributeHelper() {

    open fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    open fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.drawable.field_icon_shorttext

    open fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int = R.drawable.icon_small_shorttext
    open fun isAutoCompleteValueStatic(attribute: OTAttributeDAO): Boolean = true
    open fun isExternalFile(attribute: OTAttributeDAO): Boolean = false
    open fun getRequiredPermissions(attribute: OTAttributeDAO): Array<String>? = null
    abstract val typeNameForSerialization: String
    open val propertyKeys: Array<String> = emptyArray()

    open fun <T> parsePropertyValue(propertyKey: String, serializedValue: String): T? {
        return null
    }

    fun <T> getDeserializedPropertyValue(propertyKey: String, attribute: OTAttributeDAO): T? {
        val s = attribute.getPropertySerializedValue(propertyKey)
        return if (s != null)
            parsePropertyValue<T>(propertyKey, s)
        else null
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
}