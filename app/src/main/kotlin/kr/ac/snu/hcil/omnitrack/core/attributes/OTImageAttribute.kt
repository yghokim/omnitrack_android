package kr.ac.snu.hcil.omnitrack.core.attributes

import android.net.Uri
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
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (Uri) -> Unit): Boolean {
        resultHandler.invoke(Uri.EMPTY)
        return true
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val propertyKeys: IntArray = intArrayOf()

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_URI

    override val typeNameResourceId: Int = R.string.type_image_name

    override val typeSmallIconResourceId: Int = R.string.type_image_desc

}