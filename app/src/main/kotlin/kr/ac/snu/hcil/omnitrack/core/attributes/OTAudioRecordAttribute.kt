package kr.ac.snu.hcil.omnitrack.core.attributes

import android.net.Uri
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable

/**
 * Created by younghokim on 2016. 9. 26..
 */
class OTAudioRecordAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: String?, connectionData: String?)
    : OTAttribute<Uri>(objectId, localKey, parentTracker, columnName, isRequired, TYPE_AUDIO, settingData, connectionData) {


    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_URI

    override val typeNameResourceId: Int = R.string.type_audio_record_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_audio

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, false)

    override val propertyKeys: IntArray = intArrayOf()

    override fun createProperties() {

    }

    override fun formatAttributeValue(value: Any): CharSequence {
        //TODO read file and get the information
        return "audio"
    }

    override fun getAutoCompleteValue(): Observable<Uri> {
        return Observable.just(Uri.EMPTY)
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_AUDIO_RECORD
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

}