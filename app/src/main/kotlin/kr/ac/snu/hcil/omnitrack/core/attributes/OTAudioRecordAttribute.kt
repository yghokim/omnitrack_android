package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView
import rx.Observable
import rx.Single
import java.util.*

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
        if (inputView is AudioRecordInputView) {
            inputView.valueView.recordingOutputDirectoryPathOverride = tracker?.getItemCacheDir(inputView.context, true)
        }
    }

    override fun makeRelativeFilePathFromValue(value: Any?, uniqKey: String?): String {

        return "audios/${objectId}_${uniqKey ?: UUID.randomUUID().toString()}_audio.3gp"
    }

    override fun getViewForItemList(context: Context, recycledView: View?): View {
        val view = recycledView as? AudioItemListView ?: AudioItemListView(context)

        return view
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        if (view is AudioItemListView && value is SynchronizedUri) {
            view.mountedUri = value
        }
        return super.applyValueToViewForItemList(value, view)
    }
}