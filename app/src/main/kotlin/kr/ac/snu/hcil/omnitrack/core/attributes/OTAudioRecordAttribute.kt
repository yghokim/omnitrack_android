package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import rx.Observable

/**
 * Created by younghokim on 2016. 9. 26..
 */
class OTAudioRecordAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?)
    : OTSynchronizedUriAttribute(objectId, localKey, parentTracker, columnName, isRequired, TYPE_AUDIO, settingData, connectionData) {

    override val typeNameResourceId: Int = R.string.type_audio_record_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_audio

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, false)

    override val propertyKeys: Array<String> = emptyArray()
    
    override fun createProperties() {

    }

    override fun formatAttributeValue(value: Any): CharSequence {
        //TODO read file and get the information
        return "audio"
    }

    override fun getAutoCompleteValue(): Observable<SynchronizedUri> {
        return Observable.just(SynchronizedUri())
    }

    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_AUDIO_RECORD
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {

    }

}