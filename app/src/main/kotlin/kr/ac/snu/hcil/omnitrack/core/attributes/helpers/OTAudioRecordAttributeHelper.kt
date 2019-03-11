package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTAudioRecordAttributeHelper(context: Context) : OTFileInvolvedAttributeHelper(context) {

    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_audio_record_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_audio
    }

    override fun isExternalFile(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun getRequiredPermissions(attribute: OTAttributeDAO?): Array<String>? = permissions

    override fun formatAttributeValue(attribute: OTAttributeDAO, value: Any): CharSequence {
        return "Audio $value"
    }

    override fun makeRelativeFilePathFromValue(attribute: OTAttributeDAO, value: Any?, uniqKey: String?): String {
        return "audios/audio_${attribute.load()}_${uniqKey ?: UUID.randomUUID().toString()}.3gp"
    }
}