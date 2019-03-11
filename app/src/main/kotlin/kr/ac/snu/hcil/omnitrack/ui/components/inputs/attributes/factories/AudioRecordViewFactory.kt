package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import android.view.View
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAudioRecordAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView

class AudioRecordViewFactory(helper: OTAudioRecordAttributeHelper) : AttributeViewFactory<OTAudioRecordAttributeHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_AUDIO_RECORD

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is AudioRecordInputView) {
            inputView.valueView.recordingOutputDirectoryPathOverride = helper.localCacheManager.getDefaultItemCacheDir(attribute.trackerId
                    ?: "audios", true)
        }
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {

        return recycledView as? AudioItemListView ?: AudioItemListView(context)
    }

    override fun applyValueToViewForItemList(context: Context, attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        if (view is AudioItemListView && value is OTServerFile) {
            view.mountedServerFile = value
        }
        return super.applyValueToViewForItemList(context, attribute, value, view)
    }
}