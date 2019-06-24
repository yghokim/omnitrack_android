package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.Manifest
import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTImageFieldHelper(context: Context) : OTFileInvolvedFieldHelper(context) {

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_image_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return R.drawable.icon_small_image
    }

    override fun isExternalFile(field: OTFieldDAO): Boolean {
        return true
    }

    override fun getRequiredPermissions(field: OTFieldDAO?): Array<String>? = permissions


    override fun makeRelativeFilePathFromValue(field: OTFieldDAO, value: Any?, uniqKey: String?): String {
        return "images/image_${field.localId}_${uniqKey ?: UUID.randomUUID().toString()}.jpg"
    }

}