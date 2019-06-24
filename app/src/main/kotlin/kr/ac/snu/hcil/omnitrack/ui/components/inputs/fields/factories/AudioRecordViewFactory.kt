package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTAudioRecordFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AudioRecordInputView

class AudioRecordViewFactory(helper: OTAudioRecordFieldHelper) : OTFieldViewFactory<OTAudioRecordFieldHelper>(helper) {
    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_AUDIO_RECORD

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is AudioRecordInputView) {
            inputView.valueView.recordingOutputDirectoryPathOverride = helper.localCacheManager.getDefaultItemCacheDir(field.trackerId
                    ?: "audios", true)
        }
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {

        return recycledView as? AudioItemListView ?: AudioItemListView(context)
    }

    override fun applyValueToViewForItemList(context: Context, field: OTFieldDAO, value: Any?, view: View): Single<Boolean> {
        if (view is AudioItemListView && value is OTServerFile) {
            view.mountedServerFile = value
        }
        return super.applyValueToViewForItemList(context, field, value, view)
    }
}