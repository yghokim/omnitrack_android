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
class OTAudioRecordFieldHelper(context: Context) : OTFileInvolvedFieldHelper(context) {

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    override fun getTypeNameResourceId(field: OTFieldDAO): Int {
        return R.string.type_audio_record_name
    }

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int {
        return R.drawable.icon_small_audio
    }

    override fun isExternalFile(field: OTFieldDAO): Boolean {
        return true
    }

    override fun getRequiredPermissions(field: OTFieldDAO?): Array<String>? = permissions

    override fun formatAttributeValue(field: OTFieldDAO, value: Any): CharSequence {
        return "Audio $value"
    }

    override fun makeRelativeFilePathFromValue(field: OTFieldDAO, value: Any?, uniqKey: String?): String {
        return "audios/audio_${field.load()}_${uniqKey ?: UUID.randomUUID().toString()}.3gp"
    }
}