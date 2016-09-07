package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 16. 9. 6..
 */
class OTImageAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?)
: OTAttribute<Uri>(objectId, dbId, columnName, TYPE_IMAGE, settingData, connectionData) {


    override fun createProperties() {

    }

    override fun formatAttributeValue(value: Any): String {
        println("formatted image uri : ${value.javaClass}")
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (Uri) -> Unit): Boolean {
        resultHandler.invoke(Uri.EMPTY)
        return true
    }

    override fun getInputViewType(previewMode: Boolean): Int = AAttributeInputView.VIEW_TYPE_IMAGE

    override val propertyKeys: IntArray = intArrayOf()

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override fun getViewForItemListContainerType(): Int = VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE

    override fun getViewForItemList(context: Context, recycledView: View?): View {

        val target = if (recycledView is ImageView) {
            recycledView
        } else {
            ImageView(context)
        }

        return target
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is ImageView && value != null) {
            if (value is Uri) {
                view.setImageURI(value)
                return true
            } else return false
        } else return super.applyValueToViewForItemList(value, view)
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_URI

    override val typeNameResourceId: Int = R.string.type_image_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_image

}