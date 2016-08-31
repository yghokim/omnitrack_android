package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 16. 8. 31..
 */
class TimeQuerySettingPanel : LinearLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        inflateContent(R.layout.connection_time_query_panel, true)

    }
}