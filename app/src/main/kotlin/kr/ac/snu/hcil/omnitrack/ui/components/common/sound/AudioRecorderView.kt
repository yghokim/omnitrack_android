package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecorderView : FrameLayout {

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        inflateContent(R.layout.component_audio_recorder_view, true)
    }

}