package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTImageAttributeHelper : OTAttributeHelper() {

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_image_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_image
    }

    override fun isExternalFile(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun getRequiredPermissions(attribute: OTAttributeDAO): Array<String>? = permissions

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI
}