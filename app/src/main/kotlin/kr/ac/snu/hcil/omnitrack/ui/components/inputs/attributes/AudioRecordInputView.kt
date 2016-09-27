package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class AudioRecordInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Uri>(R.layout.input_audio_record, context, attrs) {
    override val typeId: Int = VIEW_TYPE_AUDIO_RECORD


    override var value: Uri
        get() = Uri.EMPTY
        set(value) {

        }

    init {
    }

    override fun focus() {

    }
}